package eu.ejdr.application.features.update.abstraction.repository

import eu.ejdr.application.features.update.dto.UpdateInfoDto

interface UpdateRepository {
    suspend fun fetchLatestRelease(): UpdateInfoDto?

    /**
     * Télécharge l'installeur à [url] et **garantit son intégrité avant de le rendre** :
     * l'hôte doit être un hôte GitHub de confiance et l'empreinte du binaire doit correspondre
     * à celle publiée à [sha256Url]. L'implémentation lève une exception (jamais un fichier non
     * vérifié) si [sha256Url] est `null`, l'hôte est non autorisé, ou l'empreinte diffère.
     */
    suspend fun downloadUpdate(
        url: String,
        sha256Url: String?,
        onProgress: (Float?) -> Unit,
    ): java.io.File
}
