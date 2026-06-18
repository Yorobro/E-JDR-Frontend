## [1.12.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.11.0...v1.12.0) (2026-06-18)


### Features

* **reference:** manage and use reference elements on the front ([296ff0f](https://github.com/Yorobro/E-JDR-Frontend/commit/296ff0fd24b5579883bc619abe7c79d3d0aa08cb))

## [1.11.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.10.1...v1.11.0) (2026-06-17)


### Features

* **session:** show and manage campaign sessions ([b603562](https://github.com/Yorobro/E-JDR-Frontend/commit/b6035628d979c05ccfd7394d2e73d71be8b220ad))


### Bug Fixes

* **auth:** restore session on launch and skip update check in dev ([e652ee3](https://github.com/Yorobro/E-JDR-Frontend/commit/e652ee3e8904fe3ddc4a09b5c56354f8f2c36de5))

## [1.10.1](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.10.0...v1.10.1) (2026-06-17)


### Bug Fixes

* **auth:** persist session reliably across restarts ([ff429ae](https://github.com/Yorobro/E-JDR-Frontend/commit/ff429ae88c586270d3526b4bff353a4d00c8207f))

## [1.10.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.9.0...v1.10.0) (2026-06-15)


### Features

* **config:** resolve env config at build time instead of system env ([121e732](https://github.com/Yorobro/E-JDR-Frontend/commit/121e7326c0e0551d2224bbc4c5125e7dc2bd29af))

## [1.9.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.8.0...v1.9.0) (2026-06-15)


### Features

* **auth:** require pseudo at registration without affecting login ([a22604c](https://github.com/Yorobro/E-JDR-Frontend/commit/a22604c5d58b8fa37c34556a6fc6bdd732fb5118))
* **campaign,character-sheet:** add campaigns and character sheets UI ([e8c8c02](https://github.com/Yorobro/E-JDR-Frontend/commit/e8c8c02d6b1182c04f95173d9f8ff1fd016f0932))
* **campaign:** detail page shows linkable characters, drop client-side filter ([146511a](https://github.com/Yorobro/E-JDR-Frontend/commit/146511ad71bc27e21824eda9ae6a2781d0cb71df))
* **campaign:** detail VM lists linkable characters instead of my own ([6753dbe](https://github.com/Yorobro/E-JDR-Frontend/commit/6753dbe8223b168a783eb282c776d08de0a7b8e1))
* **campaign:** render campaign card as a fixed-height tile ([b9fe2ff](https://github.com/Yorobro/E-JDR-Frontend/commit/b9fe2ff3aa7bc8ed7eb91eb3e683e016f43c1def))
* **campaign:** show campaigns in an adaptive grid with a floating add button ([87595f0](https://github.com/Yorobro/E-JDR-Frontend/commit/87595f03d7e6fa753b7b12bcb12e7ee7f1a962b5))
* **charactersheet:** add bordered SheetCard and purse normalization helper ([d635626](https://github.com/Yorobro/E-JDR-Frontend/commit/d635626106637986150228338c9f3ba4090bd214))
* **charactersheet:** add campagnes tab listing linked campaigns with mj pseudo ([be280ba](https://github.com/Yorobro/E-JDR-Frontend/commit/be280bab2a815509d9ee545afe86b2e41e696fcd))
* **charactersheet:** add export button that saves the sheet as pdf ([6bfa3c3](https://github.com/Yorobro/E-JDR-Frontend/commit/6bfa3c3bcf9a599b664204b80b195ff9e194bac8))
* **charactersheet:** add listLinkableForCampaign repo + use case + binding ([75b99ca](https://github.com/Yorobro/E-JDR-Frontend/commit/75b99ca12f27790f05538a8cc85abb0070684203))
* **charactersheet:** add pdf export data layer and desktop file saver ([2374b25](https://github.com/Yorobro/E-JDR-Frontend/commit/2374b25fe27ecf15ad0866f62db2fbcc65d60981))
* **charactersheet:** align identité fields on a regular 4-column grid ([22d4f10](https://github.com/Yorobro/E-JDR-Frontend/commit/22d4f10fd3a8e1813bd294d6f748059c249e9a8c))
* **charactersheet:** editable detail screen with full sheet fields ([1b5fc59](https://github.com/Yorobro/E-JDR-Frontend/commit/1b5fc5970a9f3249a1113710602f362103d097d8))
* **charactersheet:** grid view with fab and clickable sheet detail screen ([54862ce](https://github.com/Yorobro/E-JDR-Frontend/commit/54862ce7821db1c67f9378a5f2883879b4d1add0))
* **charactersheet:** lay out detail in bordered cards with sex dropdown and purse ([0caae86](https://github.com/Yorobro/E-JDR-Frontend/commit/0caae86192a2f246855b48e27905d9246e757ccb))
* **charactersheet:** organize detail screen into identité/combat/inventaire tabs ([31ddf61](https://github.com/Yorobro/E-JDR-Frontend/commit/31ddf618d020210d5a381b520f074080ffac1252))
* **charactersheet:** render sheet card as a clickable tile ([034b61a](https://github.com/Yorobro/E-JDR-Frontend/commit/034b61a6c33faca249cdb2458be59b2b57a29f7e))
* **charactersheet:** weighted stat-block row with stacked combat/purse and armures/armes ([6b6c6d7](https://github.com/Yorobro/E-JDR-Frontend/commit/6b6c6d79853e8888a3e89453b9fa47d3c8273501))
* **design-system:** add AppDropdown atom for closed-list choices ([c574fc9](https://github.com/Yorobro/E-JDR-Frontend/commit/c574fc903c94529653dccfd2e7ac8e98dae921c2))
* **design-system:** add AppFab floating action button ([721e28c](https://github.com/Yorobro/E-JDR-Frontend/commit/721e28cf09b533e1364f6f325bbc301aadc318b5))
* model purse, competences and integer niveau/age on front character sheet ([896a13f](https://github.com/Yorobro/E-JDR-Frontend/commit/896a13f8c5148247c1e6a621846782ca609b3af5))
* **shared:** enrich Result with map/flatMap/mapError/getOrElse/onSuccess/onFailure ([aa8a23d](https://github.com/Yorobro/E-JDR-Frontend/commit/aa8a23d27469aa6404c42f8a50d942ed2d6d1ba1))


### Bug Fixes

* **design-system:** default LocalContentColor to theme text color ([b70132e](https://github.com/Yorobro/E-JDR-Frontend/commit/b70132ec30705474104fef3b97b3ef74013d809a))
* **http:** only clear session on 401/403 refresh, keep it on transient errors ([b88f803](https://github.com/Yorobro/E-JDR-Frontend/commit/b88f80320f05bd91a0ec047679450ea904286021))
* **update:** drive UpdateViewModel via remembered scope (no ViewModelStoreOwner at root) ([d09d9cd](https://github.com/Yorobro/E-JDR-Frontend/commit/d09d9cd38298190eae51890b3b655683a41c275d))

## [1.8.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.7.0...v1.8.0) (2026-06-12)


### Features

* **settings:** handle theme persistence failures via Result railway ([0633cb8](https://github.com/Yorobro/E-JDR-Frontend/commit/0633cb865e95c38be05f6c2dd9a33d17fcfe8493))

## [1.7.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.6.0...v1.7.0) (2026-06-11)


### Features

* **realtime:** add WebSocket connection-manager (no consumer yet) ([0724465](https://github.com/Yorobro/E-JDR-Frontend/commit/0724465871d92b75debef079bf2d9c7d974d064a))


### Bug Fixes

* **navigation:** make Nav3 actually run on desktop (two runtime gaps) ([54317a3](https://github.com/Yorobro/E-JDR-Frontend/commit/54317a383aff1d475c39ecdc0892bb9b200a917e))

## [1.6.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.5.0...v1.6.0) (2026-06-11)


### Features

* **presentation:** add reusable form state-holder pattern ([b4d2cdf](https://github.com/Yorobro/E-JDR-Frontend/commit/b4d2cdfb0f0d00bb4d97bd01167492abf529155c))

## [1.5.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.4.0...v1.5.0) (2026-06-11)


### Features

* **auth:** handle ACCOUNT_LOCKED error from backend ([624136c](https://github.com/Yorobro/E-JDR-Frontend/commit/624136cf8c2a81b28935de616a7a1ff7fa67041d))

## [1.4.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.3.2...v1.4.0) (2026-06-10)


### Features

* **auth:** add GetCurrentUserUseCase wired through Koin ([df0c1df](https://github.com/Yorobro/E-JDR-Frontend/commit/df0c1df27ab2d91c7f487cb122dbbb5ceea57b55))
* **auth:** add me() to AuthRepository for protected GET /me ([572a237](https://github.com/Yorobro/E-JDR-Frontend/commit/572a2374670d7bc29a6e9f5e4f5f7002e58fe9b0))
* **home:** load current user profile from protected GET /me ([8fdcc10](https://github.com/Yorobro/E-JDR-Frontend/commit/8fdcc102013ced07ccbff4e5352596c64f9b97f7))

## [1.3.2](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.3.1...v1.3.2) (2026-06-09)


### Bug Fixes

* **ci:** unblock frontend build (detekt + broken test) ([2002e8a](https://github.com/Yorobro/E-JDR-Frontend/commit/2002e8ab71697837cb101ee08cb4e094658f0dcd))

## [1.3.1](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.3.0...v1.3.1) (2026-06-09)


### Bug Fixes

* **auth:** populate user on auto-login and activate 401 silent refresh ([02bd422](https://github.com/Yorobro/E-JDR-Frontend/commit/02bd4221588df69c7b560903ef51b13fbfca3d23))

## [1.3.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.2.0...v1.3.0) (2026-06-08)


### Features

* **theme:** add dark theme with settings page and persistence ([4e0f7a0](https://github.com/Yorobro/E-JDR-Frontend/commit/4e0f7a0c26cc673ea00ac84f8f9f70a5e580c354))
* **update:** implement download and install update use case ([4fb26fe](https://github.com/Yorobro/E-JDR-Frontend/commit/4fb26fe1060a8e95faab80a3c6b9f2358bf8b009))

## [1.2.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.1.1...v1.2.0) (2026-06-08)


### Features

* **ui:** redesign login page as centered card ([951874b](https://github.com/Yorobro/E-JDR-Frontend/commit/951874b00a585e060b5c1d3e8c46ab3c1713e17d))

## [1.1.1](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.1.0...v1.1.1) (2026-06-08)


### Bug Fixes

* set production backend URL as default ([d97d1b8](https://github.com/Yorobro/E-JDR-Frontend/commit/d97d1b886883f4f3154e2a534aa980effe0d6115))

## [1.1.0](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.0.2...v1.1.0) (2026-06-08)


### Features

* add in-app update checker via GitHub Releases API ([956c950](https://github.com/Yorobro/E-JDR-Frontend/commit/956c9500587130c37ebee2deae220488aca21a08))

## [1.0.2](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.0.1...v1.0.2) (2026-06-08)


### Bug Fixes

* merge release and cd into single workflow ([98a4841](https://github.com/Yorobro/E-JDR-Frontend/commit/98a48412791a6b789322f6123a9f98f81bead69d))
* replace cycjimmy action with npx semantic-release and tag detection ([3d21047](https://github.com/Yorobro/E-JDR-Frontend/commit/3d210472e4cba79604f5e52bc8bc698561818369))

## [1.0.1](https://github.com/Yorobro/E-JDR-Frontend/compare/v1.0.0...v1.0.1) (2026-06-08)


### Bug Fixes

* trigger release to validate new cd pipeline ([cc3212c](https://github.com/Yorobro/E-JDR-Frontend/commit/cc3212c56a440332b4f54bc97135962f034ce487))

## 1.0.0 (2026-06-08)


### Features

* ajout du cd ([d90f34a](https://github.com/Yorobro/E-JDR-Frontend/commit/d90f34aaaece8c6a3f138d536323b689bd30e6bd))
* ajout du système de version ([a297282](https://github.com/Yorobro/E-JDR-Frontend/commit/a2972829e80d43fc8da491492003635814e33867))
* ajout gestion de version plus détailler ([eadf401](https://github.com/Yorobro/E-JDR-Frontend/commit/eadf401171e81f4fa98ce4f89100a7661e161911))
* **application:** add auth abstractions (repository, service, use case interfaces) ([96dbe35](https://github.com/Yorobro/E-JDR-Frontend/commit/96dbe3554066ca58e19eac33df6e2e6faaa4f786))
* **application:** add auth use cases (login, register, restore, logout) with tests ([ad1d402](https://github.com/Yorobro/E-JDR-Frontend/commit/ad1d402efc5fd1673cd4cafaac2170108a0f6dae))
* **application:** add DefaultSessionService with tests ([54b593a](https://github.com/Yorobro/E-JDR-Frontend/commit/54b593a7862c491046d447387767523f289f4b24))
* **di:** wire infrastructure and application Koin modules ([b5e57a2](https://github.com/Yorobro/E-JDR-Frontend/commit/b5e57a2fa8a6883458fde8103ef384bb259df161))
* **domain:** add Result, DomainError, AuthError and pure auth entities ([b03d171](https://github.com/Yorobro/E-JDR-Frontend/commit/b03d171faff980e452a49b4dfe943a10aabcc46a))
* **infrastructure:** add AppConfig (base URL, data dir, logging flag) ([1564e75](https://github.com/Yorobro/E-JDR-Frontend/commit/1564e75afc0c4a42f07660397ab1f58dbb75a2e2))
* **infrastructure:** add auth DTOs, AuthHttpMapper and Ktor client factory ([09add69](https://github.com/Yorobro/E-JDR-Frontend/commit/09add6950289bb42d986a121c5af215d7c2c5baa))
* **infrastructure:** add AuthHttpRepository against /auth endpoints ([b425f1d](https://github.com/Yorobro/E-JDR-Frontend/commit/b425f1d634cf71146483984d8a1c33728e148b00))
* **infrastructure:** add KeyStore-backed AES-GCM cookie cipher with tests ([5649719](https://github.com/Yorobro/E-JDR-Frontend/commit/564971946bea88ef7df6e514e58a69cb5a58efd6))
* **infrastructure:** add SecureCookiesStorage persisting encrypted refresh_token ([995eab3](https://github.com/Yorobro/E-JDR-Frontend/commit/995eab33cc368235bca2da72e6b1f61dda8a211c))
* **presentation:** add AppNumberField with tested numeric filtering ([2065b26](https://github.com/Yorobro/E-JDR-Frontend/commit/2065b269c498ccdd24500b1ff1b38d6c0ecf2196))
* **presentation:** add AppPasswordField with visibility toggle ([b7cfff7](https://github.com/Yorobro/E-JDR-Frontend/commit/b7cfff75759895d99fab1a6b1a6956b85d4e1c94))
* **presentation:** add AppText atom ([a931c57](https://github.com/Yorobro/E-JDR-Frontend/commit/a931c57ec7abab0f81f032b384ef864a9897099a))
* **presentation:** add design system theme (colors, typography, dimens) ([67a03ec](https://github.com/Yorobro/E-JDR-Frontend/commit/67a03ec091b24b1be230325fcea717546917fda0))
* **presentation:** add dumb LoginForm and RegisterForm components ([b642f71](https://github.com/Yorobro/E-JDR-Frontend/commit/b642f7112cc7a32790abd0b7d1dd17b5537efce5))
* **presentation:** add LabeledField and FieldGroup molecules ([bb87f98](https://github.com/Yorobro/E-JDR-Frontend/commit/bb87f98be798742185f1af186deb558bcaabdcd5))
* **presentation:** add navigation, app root with auto-login and wire main ([bda11e6](https://github.com/Yorobro/E-JDR-Frontend/commit/bda11e62415eb60a6cc4a9328c0f88461aee9014))
* **presentation:** add remaining design system atoms ([716b6fb](https://github.com/Yorobro/E-JDR-Frontend/commit/716b6fb4e8acfcef0d2c255110b58140c6684a41))
* **presentation:** add shared atomic-design components (button, textfield, error) ([2f07da5](https://github.com/Yorobro/E-JDR-Frontend/commit/2f07da593e00e089c17bd03fb7dbe19e223d9867))
* **presentation:** add smart LoginPage and RegisterPage calling use cases ([9e72d0c](https://github.com/Yorobro/E-JDR-Frontend/commit/9e72d0c780c5b041a062b8cc0f7ab2d955af6e3a))
* **presentation:** add top bar and app scaffold ([120f924](https://github.com/Yorobro/E-JDR-Frontend/commit/120f924b7e90c7e74b2fdd0fc2d7e1c40c265dc9))
* **presentation:** add user page ([fb8adc2](https://github.com/Yorobro/E-JDR-Frontend/commit/fb8adc20efcba5087b4f9084d0b6a2ffa041b840))
* **presentation:** rework AppButton with 5 themed variants ([b689b3b](https://github.com/Yorobro/E-JDR-Frontend/commit/b689b3bbbf5eddbc7732444dd9a64dda123794ee))
* **presentation:** rework AppTextField as themed outlined field ([904b3f4](https://github.com/Yorobro/E-JDR-Frontend/commit/904b3f485db0b102ee175db9914a97c9a428c45f))
* **security,quality:** durcissement KeyStore, contrat d'erreur, detekt/Kover + résilience ([f8c8af6](https://github.com/Yorobro/E-JDR-Frontend/commit/f8c8af6d68f92eb02e5c5fe77220beca1c0fa777))


### Bug Fixes

* cicd ([42dca68](https://github.com/Yorobro/E-JDR-Frontend/commit/42dca6807acdfcfa1b29fd8dd4b9535ba9f37f05))
* deploy ([a68bd3c](https://github.com/Yorobro/E-JDR-Frontend/commit/a68bd3c04e1d58cea98405dbc170b42508af8229))
* deploy certain files ([64b12d3](https://github.com/Yorobro/E-JDR-Frontend/commit/64b12d3c8675afd5d80692889c67d1889b5e2821))
* **github-script:** remove duplicate 'core' declaration ([cf054c7](https://github.com/Yorobro/E-JDR-Frontend/commit/cf054c7ce8649d132eeb84241489aef006f42144))
* **github-script:** use context.payload.release and guard missing tag ([4869520](https://github.com/Yorobro/E-JDR-Frontend/commit/48695201a1556762871f6c92fa0e5a97785a3a46))
* **presentation:** correct expected value in numeric filter test ([aa3becd](https://github.com/Yorobro/E-JDR-Frontend/commit/aa3becdd5d41dd1439fc826d18de34720b2753bc))

## 1.0.0 (2026-06-08)


### Features

* ajout du cd ([d90f34a](https://github.com/Yorobro/E-JDR-Frontend/commit/d90f34aaaece8c6a3f138d536323b689bd30e6bd))
* ajout du système de version ([a297282](https://github.com/Yorobro/E-JDR-Frontend/commit/a2972829e80d43fc8da491492003635814e33867))
* ajout gestion de version plus détailler ([eadf401](https://github.com/Yorobro/E-JDR-Frontend/commit/eadf401171e81f4fa98ce4f89100a7661e161911))
* **application:** add auth abstractions (repository, service, use case interfaces) ([96dbe35](https://github.com/Yorobro/E-JDR-Frontend/commit/96dbe3554066ca58e19eac33df6e2e6faaa4f786))
* **application:** add auth use cases (login, register, restore, logout) with tests ([ad1d402](https://github.com/Yorobro/E-JDR-Frontend/commit/ad1d402efc5fd1673cd4cafaac2170108a0f6dae))
* **application:** add DefaultSessionService with tests ([54b593a](https://github.com/Yorobro/E-JDR-Frontend/commit/54b593a7862c491046d447387767523f289f4b24))
* **di:** wire infrastructure and application Koin modules ([b5e57a2](https://github.com/Yorobro/E-JDR-Frontend/commit/b5e57a2fa8a6883458fde8103ef384bb259df161))
* **domain:** add Result, DomainError, AuthError and pure auth entities ([b03d171](https://github.com/Yorobro/E-JDR-Frontend/commit/b03d171faff980e452a49b4dfe943a10aabcc46a))
* **infrastructure:** add AppConfig (base URL, data dir, logging flag) ([1564e75](https://github.com/Yorobro/E-JDR-Frontend/commit/1564e75afc0c4a42f07660397ab1f58dbb75a2e2))
* **infrastructure:** add auth DTOs, AuthHttpMapper and Ktor client factory ([09add69](https://github.com/Yorobro/E-JDR-Frontend/commit/09add6950289bb42d986a121c5af215d7c2c5baa))
* **infrastructure:** add AuthHttpRepository against /auth endpoints ([b425f1d](https://github.com/Yorobro/E-JDR-Frontend/commit/b425f1d634cf71146483984d8a1c33728e148b00))
* **infrastructure:** add KeyStore-backed AES-GCM cookie cipher with tests ([5649719](https://github.com/Yorobro/E-JDR-Frontend/commit/564971946bea88ef7df6e514e58a69cb5a58efd6))
* **infrastructure:** add SecureCookiesStorage persisting encrypted refresh_token ([995eab3](https://github.com/Yorobro/E-JDR-Frontend/commit/995eab33cc368235bca2da72e6b1f61dda8a211c))
* **presentation:** add AppNumberField with tested numeric filtering ([2065b26](https://github.com/Yorobro/E-JDR-Frontend/commit/2065b269c498ccdd24500b1ff1b38d6c0ecf2196))
* **presentation:** add AppPasswordField with visibility toggle ([b7cfff7](https://github.com/Yorobro/E-JDR-Frontend/commit/b7cfff75759895d99fab1a6b1a6956b85d4e1c94))
* **presentation:** add AppText atom ([a931c57](https://github.com/Yorobro/E-JDR-Frontend/commit/a931c57ec7abab0f81f032b384ef864a9897099a))
* **presentation:** add design system theme (colors, typography, dimens) ([67a03ec](https://github.com/Yorobro/E-JDR-Frontend/commit/67a03ec091b24b1be230325fcea717546917fda0))
* **presentation:** add dumb LoginForm and RegisterForm components ([b642f71](https://github.com/Yorobro/E-JDR-Frontend/commit/b642f7112cc7a32790abd0b7d1dd17b5537efce5))
* **presentation:** add LabeledField and FieldGroup molecules ([bb87f98](https://github.com/Yorobro/E-JDR-Frontend/commit/bb87f98be798742185f1af186deb558bcaabdcd5))
* **presentation:** add navigation, app root with auto-login and wire main ([bda11e6](https://github.com/Yorobro/E-JDR-Frontend/commit/bda11e62415eb60a6cc4a9328c0f88461aee9014))
* **presentation:** add remaining design system atoms ([716b6fb](https://github.com/Yorobro/E-JDR-Frontend/commit/716b6fb4e8acfcef0d2c255110b58140c6684a41))
* **presentation:** add shared atomic-design components (button, textfield, error) ([2f07da5](https://github.com/Yorobro/E-JDR-Frontend/commit/2f07da593e00e089c17bd03fb7dbe19e223d9867))
* **presentation:** add smart LoginPage and RegisterPage calling use cases ([9e72d0c](https://github.com/Yorobro/E-JDR-Frontend/commit/9e72d0c780c5b041a062b8cc0f7ab2d955af6e3a))
* **presentation:** add top bar and app scaffold ([120f924](https://github.com/Yorobro/E-JDR-Frontend/commit/120f924b7e90c7e74b2fdd0fc2d7e1c40c265dc9))
* **presentation:** add user page ([fb8adc2](https://github.com/Yorobro/E-JDR-Frontend/commit/fb8adc20efcba5087b4f9084d0b6a2ffa041b840))
* **presentation:** rework AppButton with 5 themed variants ([b689b3b](https://github.com/Yorobro/E-JDR-Frontend/commit/b689b3bbbf5eddbc7732444dd9a64dda123794ee))
* **presentation:** rework AppTextField as themed outlined field ([904b3f4](https://github.com/Yorobro/E-JDR-Frontend/commit/904b3f485db0b102ee175db9914a97c9a428c45f))
* **security,quality:** durcissement KeyStore, contrat d'erreur, detekt/Kover + résilience ([f8c8af6](https://github.com/Yorobro/E-JDR-Frontend/commit/f8c8af6d68f92eb02e5c5fe77220beca1c0fa777))


### Bug Fixes

* cicd ([42dca68](https://github.com/Yorobro/E-JDR-Frontend/commit/42dca6807acdfcfa1b29fd8dd4b9535ba9f37f05))
* deploy ([a68bd3c](https://github.com/Yorobro/E-JDR-Frontend/commit/a68bd3c04e1d58cea98405dbc170b42508af8229))
* deploy certain files ([64b12d3](https://github.com/Yorobro/E-JDR-Frontend/commit/64b12d3c8675afd5d80692889c67d1889b5e2821))
* **github-script:** remove duplicate 'core' declaration ([cf054c7](https://github.com/Yorobro/E-JDR-Frontend/commit/cf054c7ce8649d132eeb84241489aef006f42144))
* **presentation:** correct expected value in numeric filter test ([aa3becd](https://github.com/Yorobro/E-JDR-Frontend/commit/aa3becdd5d41dd1439fc826d18de34720b2753bc))

## 1.0.0 (2026-06-08)


### Features

* ajout du cd ([d90f34a](https://github.com/Yorobro/E-JDR-Frontend/commit/d90f34aaaece8c6a3f138d536323b689bd30e6bd))
* ajout du système de version ([a297282](https://github.com/Yorobro/E-JDR-Frontend/commit/a2972829e80d43fc8da491492003635814e33867))
* ajout gestion de version plus détailler ([eadf401](https://github.com/Yorobro/E-JDR-Frontend/commit/eadf401171e81f4fa98ce4f89100a7661e161911))
* **application:** add auth abstractions (repository, service, use case interfaces) ([96dbe35](https://github.com/Yorobro/E-JDR-Frontend/commit/96dbe3554066ca58e19eac33df6e2e6faaa4f786))
* **application:** add auth use cases (login, register, restore, logout) with tests ([ad1d402](https://github.com/Yorobro/E-JDR-Frontend/commit/ad1d402efc5fd1673cd4cafaac2170108a0f6dae))
* **application:** add DefaultSessionService with tests ([54b593a](https://github.com/Yorobro/E-JDR-Frontend/commit/54b593a7862c491046d447387767523f289f4b24))
* **di:** wire infrastructure and application Koin modules ([b5e57a2](https://github.com/Yorobro/E-JDR-Frontend/commit/b5e57a2fa8a6883458fde8103ef384bb259df161))
* **domain:** add Result, DomainError, AuthError and pure auth entities ([b03d171](https://github.com/Yorobro/E-JDR-Frontend/commit/b03d171faff980e452a49b4dfe943a10aabcc46a))
* **infrastructure:** add AppConfig (base URL, data dir, logging flag) ([1564e75](https://github.com/Yorobro/E-JDR-Frontend/commit/1564e75afc0c4a42f07660397ab1f58dbb75a2e2))
* **infrastructure:** add auth DTOs, AuthHttpMapper and Ktor client factory ([09add69](https://github.com/Yorobro/E-JDR-Frontend/commit/09add6950289bb42d986a121c5af215d7c2c5baa))
* **infrastructure:** add AuthHttpRepository against /auth endpoints ([b425f1d](https://github.com/Yorobro/E-JDR-Frontend/commit/b425f1d634cf71146483984d8a1c33728e148b00))
* **infrastructure:** add KeyStore-backed AES-GCM cookie cipher with tests ([5649719](https://github.com/Yorobro/E-JDR-Frontend/commit/564971946bea88ef7df6e514e58a69cb5a58efd6))
* **infrastructure:** add SecureCookiesStorage persisting encrypted refresh_token ([995eab3](https://github.com/Yorobro/E-JDR-Frontend/commit/995eab33cc368235bca2da72e6b1f61dda8a211c))
* **presentation:** add AppNumberField with tested numeric filtering ([2065b26](https://github.com/Yorobro/E-JDR-Frontend/commit/2065b269c498ccdd24500b1ff1b38d6c0ecf2196))
* **presentation:** add AppPasswordField with visibility toggle ([b7cfff7](https://github.com/Yorobro/E-JDR-Frontend/commit/b7cfff75759895d99fab1a6b1a6956b85d4e1c94))
* **presentation:** add AppText atom ([a931c57](https://github.com/Yorobro/E-JDR-Frontend/commit/a931c57ec7abab0f81f032b384ef864a9897099a))
* **presentation:** add design system theme (colors, typography, dimens) ([67a03ec](https://github.com/Yorobro/E-JDR-Frontend/commit/67a03ec091b24b1be230325fcea717546917fda0))
* **presentation:** add dumb LoginForm and RegisterForm components ([b642f71](https://github.com/Yorobro/E-JDR-Frontend/commit/b642f7112cc7a32790abd0b7d1dd17b5537efce5))
* **presentation:** add LabeledField and FieldGroup molecules ([bb87f98](https://github.com/Yorobro/E-JDR-Frontend/commit/bb87f98be798742185f1af186deb558bcaabdcd5))
* **presentation:** add navigation, app root with auto-login and wire main ([bda11e6](https://github.com/Yorobro/E-JDR-Frontend/commit/bda11e62415eb60a6cc4a9328c0f88461aee9014))
* **presentation:** add remaining design system atoms ([716b6fb](https://github.com/Yorobro/E-JDR-Frontend/commit/716b6fb4e8acfcef0d2c255110b58140c6684a41))
* **presentation:** add shared atomic-design components (button, textfield, error) ([2f07da5](https://github.com/Yorobro/E-JDR-Frontend/commit/2f07da593e00e089c17bd03fb7dbe19e223d9867))
* **presentation:** add smart LoginPage and RegisterPage calling use cases ([9e72d0c](https://github.com/Yorobro/E-JDR-Frontend/commit/9e72d0c780c5b041a062b8cc0f7ab2d955af6e3a))
* **presentation:** add top bar and app scaffold ([120f924](https://github.com/Yorobro/E-JDR-Frontend/commit/120f924b7e90c7e74b2fdd0fc2d7e1c40c265dc9))
* **presentation:** add user page ([fb8adc2](https://github.com/Yorobro/E-JDR-Frontend/commit/fb8adc20efcba5087b4f9084d0b6a2ffa041b840))
* **presentation:** rework AppButton with 5 themed variants ([b689b3b](https://github.com/Yorobro/E-JDR-Frontend/commit/b689b3bbbf5eddbc7732444dd9a64dda123794ee))
* **presentation:** rework AppTextField as themed outlined field ([904b3f4](https://github.com/Yorobro/E-JDR-Frontend/commit/904b3f485db0b102ee175db9914a97c9a428c45f))
* **security,quality:** durcissement KeyStore, contrat d'erreur, detekt/Kover + résilience ([f8c8af6](https://github.com/Yorobro/E-JDR-Frontend/commit/f8c8af6d68f92eb02e5c5fe77220beca1c0fa777))


### Bug Fixes

* cicd ([42dca68](https://github.com/Yorobro/E-JDR-Frontend/commit/42dca6807acdfcfa1b29fd8dd4b9535ba9f37f05))
* deploy ([a68bd3c](https://github.com/Yorobro/E-JDR-Frontend/commit/a68bd3c04e1d58cea98405dbc170b42508af8229))
* deploy certain files ([64b12d3](https://github.com/Yorobro/E-JDR-Frontend/commit/64b12d3c8675afd5d80692889c67d1889b5e2821))
* **presentation:** correct expected value in numeric filter test ([aa3becd](https://github.com/Yorobro/E-JDR-Frontend/commit/aa3becdd5d41dd1439fc826d18de34720b2753bc))
