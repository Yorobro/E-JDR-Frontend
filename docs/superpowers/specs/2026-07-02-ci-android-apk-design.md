# Design — Publier l'APK Android dans la release CI/CD

**Date** : 2026-07-02
**Branche** : `feat/ci-android-apk` (depuis `main`)
**Fichier concerné** : `.github/workflows/release.yml`
**Statut** : validé, prêt pour plan d'implémentation

## Objectif

En plus des installeurs Windows (EXE/MSI), le workflow de release doit builder un **APK Android
debug** et l'**attacher à la release GitHub**, pour l'installer directement sur téléphone.
L'APK pointe automatiquement sur la **PROD**.

## Contexte existant (ne change pas)

- `release.yml` s'exécute sur push `main` : `ci` → `semantic_release` (crée le tag/release) →
  `windows_build` (build EXE/MSI, checksums SHA-256, attache à la release au tag).
- Le job `windows_build` est gardé par `needs: semantic_release` +
  `if: needs.semantic_release.outputs.released == 'true'` (ne build que sur vraie release).
- **Résolution de l'URL d'API** (build.gradle.kts) : le build lit `config.defaults.properties`
  puis `config.local.properties` (ce dernier écrase). `api.url` est injecté dans
  `BuildConfig.API_URL`.
  - `config.defaults.properties` → `api.url=https://ejdr-backend.vyxs.fr` (**PROD**).
  - `config.local.properties` → dev, mais **gitignore** (`.gitignore` ligne 10) et **non
    commité** → **absent sur GitHub Actions**.
  - **Conséquence** : sur le CI, seul `defaults` s'applique → **tout APK CI pointe sur la PROD**,
    sans aucune manipulation d'URL. La contrainte principale est déjà satisfaite par
    l'architecture existante.
- Le plugin `com.android.application` est configuré ; `assembleDebug` produit déjà un APK
  (cf. migration KMP). Aucune `signingConfig` n'existe → seul le **debug** (clé debug auto)
  est installable directement.

## Décision : job `android_build` dédié, parallèle à `windows_build`

On **ajoute** un job `android_build` (on ne touche pas `windows_build`), calqué sur lui.

Raisons :
- **Runner `ubuntu-latest`** : build Android plus rapide/standard et moins cher que Windows.
- **Isolation** : un échec du build APK ne casse pas la publication des binaires Windows
  (et inversement). Artefacts indépendants.
- Même pattern que l'existant (checkout au tag → JDK 21 → Gradle → build → attacher à la release).

## Le job (contenu)

```yaml
android_build:
  name: Build and publish Android APK
  needs: semantic_release
  if: ${{ needs.semantic_release.outputs.released == 'true' }}
  runs-on: ubuntu-latest
  steps:
    - name: Checkout at release tag
      uses: actions/checkout@v4
      with:
        ref: ${{ needs.semantic_release.outputs.tag }}

    - name: Setup JDK 21
      uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: "21"

    - name: Setup Gradle cache
      uses: gradle/actions/setup-gradle@v4

    - name: Make gradlew executable
      run: chmod +x ./gradlew

    - name: Build debug APK
      run: ./gradlew assembleDebug --no-daemon --stacktrace

    - name: Rename APK with version
      run: |
        APK=$(find build -path '*/apk/debug/*.apk' | head -1)
        cp "$APK" "E-JDR-${{ needs.semantic_release.outputs.tag }}.apk"

    - name: Attach APK to release
      uses: softprops/action-gh-release@v2
      with:
        tag_name: ${{ needs.semantic_release.outputs.tag }}
        files: E-JDR-${{ needs.semantic_release.outputs.tag }}.apk
```

## Décisions de détail

- **`assembleDebug`** : APK signé (clé debug), installable directement. Sortie *attendue*
  `build/outputs/apk/debug/*.apk` (à **confirmer localement** avant merge). Le step utilise
  `find build -path '*/apk/debug/*.apk'` : robuste même si le chemin exact diffère, tant que
  l'APK debug est sous un dossier `.../apk/debug/`. Si l'APK sort ailleurs, ajuster le motif.
- **Renommage** en `E-JDR-<tag>.apk` (ex. `E-JDR-v1.16.0.apk`) : asset de release clair et
  versionné (au lieu du `<module>-debug.apk` par défaut).
- **Pas de checksum SHA-256** pour l'APK (contrairement aux binaires Windows dont l'auto-update
  vérifie l'empreinte) : l'APK n'a pas de mécanisme d'auto-update qui l'exige. YAGNI ;
  ajoutable plus tard.
- **`windows_build` inchangé.**

## Hors périmètre (YAGNI)

- Pas d'APK release signé (pas de keystore/secret GitHub à créer) — envisageable dans un 2e temps.
- Pas de bundle AAB / publication Play Store.
- Pas de checksum ni d'auto-update Android.
- Pas de modification de l'URL d'API (déjà prod via defaults au CI).

## Vérification

- Modif purement CI : validation locale = lint YAML + relecture. Le vrai test est le
  **prochain run de release** sur `main`.
- Critère de succès : à la prochaine release, un asset `E-JDR-<tag>.apk` apparaît sur la release
  GitHub, à côté des EXE/MSI, installable sur un téléphone et pointant sur la prod.
- Optionnel avant merge : valider localement que `./gradlew assembleDebug` produit bien un APK
  (déjà connu OK d'après les notes KMP) et confirmer le chemin de sortie
  (`build/outputs/apk/debug/`).
