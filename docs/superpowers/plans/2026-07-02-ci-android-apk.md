# Publier l'APK Android dans la release CI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter au workflow de release un job qui build l'APK Android debug et l'attache à la release GitHub, pour l'installer directement sur téléphone (pointe sur la prod).

**Architecture:** Un nouveau job `android_build` dans `.github/workflows/release.yml`, parallèle et indépendant de `windows_build`, gardé par les mêmes conditions (`needs: semantic_release` + `if released == 'true'`). Il tourne sur `ubuntu-latest`, build via `assembleDebug`, renomme l'APK `E-JDR-<tag>.apk` et l'attache à la release au tag. Aucun code applicatif ni autre job n'est modifié.

**Tech Stack:** GitHub Actions, Gradle (Android `com.android.application`), `assembleDebug`, `softprops/action-gh-release@v2`.

## Global Constraints

- Fichier modifié : **uniquement** `.github/workflows/release.yml` (ajout d'un job, rien d'autre touché).
- Job Android : `runs-on: ubuntu-latest`, `needs: semantic_release`, `if: ${{ needs.semantic_release.outputs.released == 'true' }}`.
- Build : `./gradlew assembleDebug --no-daemon --stacktrace`.
- Chemin de sortie APK **confirmé localement** : `build/outputs/apk/debug/ejdr-frontend-debug.apk`. Le step le localise via `find build -path '*/apk/debug/*.apk' | head -1` (robuste).
- Nom d'asset attaché à la release : **`E-JDR-<tag>.apk`** (ex. `E-JDR-v1.16.0.apk`), où `<tag>` = `${{ needs.semantic_release.outputs.tag }}`.
- **Pas** de checksum SHA-256, **pas** de keystore/signature release, **pas** de modification de l'URL d'API (déjà prod via `config.defaults.properties`, `config.local.properties` étant gitignore/absent du CI).
- Le job `windows_build` existant **n'est pas modifié**.
- JDK 21 (temurin), actions : `actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`, `softprops/action-gh-release@v2` (mêmes versions que le reste du fichier).

---

### Task 1 : Ajouter le job `android_build` à release.yml

**Files:**
- Modify: `.github/workflows/release.yml` (ajout d'un job à la fin, après le job `windows_build` qui se termine actuellement ligne 123)

**Interfaces:**
- Consumes: la sortie `needs.semantic_release.outputs.tag` et `needs.semantic_release.outputs.released` (déjà définies par le job `semantic_release`, exactement comme `windows_build` les consomme).
- Produces: un asset `E-JDR-<tag>.apk` attaché à la release GitHub du tag. Aucune sortie consommée par un autre job.

- [ ] **Step 1 : Vérifier l'état de départ du fichier**

Run: `tail -15 .github/workflows/release.yml`
Expected: le fichier se termine par le step « Attach binaries to release » du job `windows_build` (bloc `files:` listant les `*.exe`, `*.msi` et leurs `.sha256`). Noter l'indentation : les jobs sont à 2 espaces (`  windows_build:`), leurs clés à 4 espaces.

- [ ] **Step 2 : Ajouter le job `android_build` à la fin du fichier**

Ajouter ce bloc à la **fin** de `.github/workflows/release.yml` (après la dernière ligne du job `windows_build`), en respectant l'indentation à 2 espaces pour le nom du job (même niveau que `windows_build:`) :

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

      # L'APK sort dans build/outputs/apk/debug/ejdr-frontend-debug.apk (confirmé localement).
      # On le copie sous un nom versionné et clair pour l'asset de release.
      - name: Rename APK with version
        run: |
          APK=$(find build -path '*/apk/debug/*.apk' | head -1)
          if [ -z "$APK" ]; then echo "APK debug introuvable"; exit 1; fi
          cp "$APK" "E-JDR-${{ needs.semantic_release.outputs.tag }}.apk"

      - name: Attach APK to release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ needs.semantic_release.outputs.tag }}
          files: E-JDR-${{ needs.semantic_release.outputs.tag }}.apk
```

Note : le `if [ -z "$APK" ]; then ... exit 1; fi` fait échouer le job explicitement (message clair) si aucun APK n'est trouvé, plutôt qu'un `cp` cryptique.

- [ ] **Step 3 : Vérifier que le YAML est valide**

Run:
```bash
python -c "import yaml,sys; d=yaml.safe_load(open('.github/workflows/release.yml')); print('jobs:', list(d['jobs'].keys()))"
```
Expected : affiche `jobs: ['ci', 'semantic_release', 'windows_build', 'android_build']` — le nouveau job est présent et le YAML parse sans erreur.

(Si `python`/`yaml` indisponible, fallback : `npx --yes js-yaml .github/workflows/release.yml >/dev/null && echo "YAML OK"` → doit afficher `YAML OK`.)

- [ ] **Step 4 : Vérifier que windows_build est intact**

Run: `git diff .github/workflows/release.yml`
Expected : le diff ne montre **que des ajouts** (le bloc `android_build`), aucune ligne du job `windows_build` ni des jobs `ci`/`semantic_release` supprimée ou modifiée.

- [ ] **Step 5 : Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: publier l'APK Android (debug) dans la release GitHub"
```

---

## Self-Review

**Spec coverage** (chaque exigence de la spec → une tâche) :
- Job `android_build` parallèle à `windows_build`, gardes `needs`/`if`, `ubuntu-latest` → **Task 1** ✅
- `assembleDebug`, renommage `E-JDR-<tag>.apk`, attache à la release → **Task 1** ✅
- Pas de checksum / keystore / modif URL → aucune tâche ne les ajoute ✅
- `windows_build` inchangé → vérifié explicitement Task 1 Step 4 ✅
- Chemin APK confirmé (`build/outputs/apk/debug/ejdr-frontend-debug.apk`) → codé via `find`, mentionné en contrainte ✅

**Placeholder scan** : aucun TBD/TODO ; le bloc YAML complet est fourni. ✅

**Type/nom consistency** : noms constants — job `android_build`, asset `E-JDR-<tag>.apk`, `needs.semantic_release.outputs.tag`/`.released`, motif `*/apk/debug/*.apk`. Cohérents avec la spec et avec l'usage réel dans `windows_build`. ✅

**Note sur les tests** : cette modif est purement CI et ne peut pas être « testée » unitairement en local — la validation réelle est le prochain run de release sur `main` (un asset `.apk` doit apparaître). Les steps 3-4 valident ce qui est vérifiable sans déclencher un run (YAML valide, non-régression du fichier). L'APK debug lui-même a déjà été build localement avec succès pendant la préparation du plan.
