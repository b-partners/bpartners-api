## [0.78.1](https://github.com/b-partners/bpartners-api/compare/v0.78.0...v0.78.1) (2026-02-03)


### Bug Fixes

* commit untracked files on releases-version ([489aa75](https://github.com/b-partners/bpartners-api/commit/489aa7534da183c9ae0ad544d4e9ffabce878087))



# [0.78.0](https://github.com/b-partners/bpartners-api/compare/v0.77.0...v0.78.0) (2026-02-03)


### Bug Fixes

* do not look for defaut payment only existing payment methods ([aa8c56e](https://github.com/b-partners/bpartners-api/commit/aa8c56e692f86c7c344e31958e4d668852703188))
* handle multiple analysis api key found by one key ([a20aac7](https://github.com/b-partners/bpartners-api/commit/a20aac7d2c53702b3f1b734db8864d9e94ceba26))
* handle one by one user subscription invoice computing ([c2a7488](https://github.com/b-partners/bpartners-api/commit/c2a7488679bc3952453cea6c185c3e58ea896116))
* **MonthlySubscriptionInvoiceRequestedService:** do not compute subscription invoice when already computed for current month ([7c47057](https://github.com/b-partners/bpartners-api/commit/7c47057506ade869d542f1975e75cb209e010571))
* **MonthlySubscriptionInvoiceRequestedService:** filter existing subscritpion invoices using userToCredit and userToDebit filter ([18e1b69](https://github.com/b-partners/bpartners-api/commit/18e1b69e3980c21a857619442ead2e6ab743b6ad))
* **MonthlySubscriptionInvoiceRequestedService:** only compute subscription invoice for active subscription and eligible user ([dd0bfe1](https://github.com/b-partners/bpartners-api/commit/dd0bfe17bce30c20c885d35755a2e54191d62803))
* **MonthlySubscriptionInvoiceRequestedService:** set default sending date to last day of actual month ([20cb8a5](https://github.com/b-partners/bpartners-api/commit/20cb8a5246297c4f5acef0ea7216aacc0ef21cd5))
* **ProspectService:** trigger ProspectUpdate event on each saving methods ([ce8aba1](https://github.com/b-partners/bpartners-api/commit/ce8aba141b6cee4b85cdf4c392c3f56a4b9e7b63))
* **RefreshUserInvoiceSummaryTriggered:** update eventStack=EVENT_STACK_2 ([a0eed3a](https://github.com/b-partners/bpartners-api/commit/a0eed3af1564487b2ae9820406f78c4903840f6a))
* retrieve payment methods from both subscription and customer ([f81d4ee](https://github.com/b-partners/bpartners-api/commit/f81d4ee6b18ea689b3462ba060c2ba4a6116883d))
* **StripeFactory:** avoid billing_cycle_anchor late than natural billing by handling today if before fifth of actual month ([63be005](https://github.com/b-partners/bpartners-api/commit/63be005ab98e592d15e4ca87eb74b0222f71ea6a))
* **SubscriptionService:** update free trial period to 7 days ([ea78623](https://github.com/b-partners/bpartners-api/commit/ea78623fa5289f390b0cfa9d058e724f2133381e))
* use upcoming stripe invoice to compute subscription invoice ([e8b42ac](https://github.com/b-partners/bpartners-api/commit/e8b42ac581a974d1649abd0f04ac339d56bd4eb6))
* **UserController:** type updated user api keys as DASHBOARD and add default creation datetime ([6e415ae](https://github.com/b-partners/bpartners-api/commit/6e415aecc244647c9eb2b7aedd2ebf4e0705ad89))
* **UserOnboarded:** generate api key after user onboarded ([f9f6298](https://github.com/b-partners/bpartners-api/commit/f9f629884798b0eaf639c00f6ddd95aa0216f0f7))
* **UserRestMapper:** always return ACTIVE when not subscription eligibile ([1bc6474](https://github.com/b-partners/bpartners-api/commit/1bc64741706fb145bd277d65e003a06105212351))
* **UserRestMapper:** return ACTIVE only when free trial period not active ([e6940d6](https://github.com/b-partners/bpartners-api/commit/e6940d6f64d9658d17e6023076a34a9dd29530ed))
* **UserSubscription:** only subscription not expired can be valid ([19dba5e](https://github.com/b-partners/bpartners-api/commit/19dba5e7125538acca229b7b386a231ae4a61fe0))


### Features

* implement areaPicture shiftDirection ([67a1ec7](https://github.com/b-partners/bpartners-api/commit/67a1ec74d7a9cb824b20c3f3f35bf51c85b3b0f6))


### Reverts

* Revert "chore(to-revert): update invoice trigger information to december 2025" ([4ed25bf](https://github.com/b-partners/bpartners-api/commit/4ed25bf6c23283ccf4dd193f412dbae8985db5a7))



# [0.77.0](https://github.com/b-partners/bpartners-api/compare/v0.76.0...v0.77.0) (2026-01-29)


### Bug Fixes

* accept only not expired card as default payment method ([96ecc48](https://github.com/b-partners/bpartners-api/commit/96ecc48f0c3fbc8f49afe6d59cf65339d289c8ba))
* **DbContextInitializer:** setup 60s startup timeout to prevent flyway launch before postgres ([bd076b8](https://github.com/b-partners/bpartners-api/commit/bd076b8adc3151a3963414765ba6b67f4f5d62cd))
* handle User and UserAnalysisApiKey entities through unidirectionnal relation ([294691f](https://github.com/b-partners/bpartners-api/commit/294691f57abc4745759c5d1e80c9156d79436e55))
* handle user payment method verification when trial period expired ([e072c6c](https://github.com/b-partners/bpartners-api/commit/e072c6cd3b309a3bfb2be9162af2c9d2a2a0c4b2))
* **ProspectJpaRepository:** filter existing prospects by idAccountHolder and (old email or new email) ([c420fda](https://github.com/b-partners/bpartners-api/commit/c420fda75e8dec01f424dd1713f47e59f333db27))
* **ProspectJpaRepository:** filter existing prospects by idAccountHolder and (old email or new email) ([0de03f0](https://github.com/b-partners/bpartners-api/commit/0de03f0e2813be459fc59af44107b6e4d05b7578))
* **ProspectJpaRepository:** reverse args on filtering existing account holder prospects ([564d177](https://github.com/b-partners/bpartners-api/commit/564d177f6dc8511faf6c9fdab82974ff253a1312))
* **ProspectJpaRepository:** reverse args on filtering existing account holder prospects ([ad009ed](https://github.com/b-partners/bpartners-api/commit/ad009ed70e46fae7058078b02b6e98d2a922129f))
* **RefreshInvoiceSummaryTriggeredService:** filter users to compute invoice summary to those associated to stripe only ([5daf7d2](https://github.com/b-partners/bpartners-api/commit/5daf7d2b7325ffb3b723d530832563baf1bb0724))
* **SecurityConf:** only ADMIN can view users keys ([73dd577](https://github.com/b-partners/bpartners-api/commit/73dd57715ff61a3aa1e6dc7178d560bb4881d6b8))
* **UserAnalysisApiKey:** ignore User attribute to avoid infinite loop ([2009823](https://github.com/b-partners/bpartners-api/commit/2009823c738bbb67815eb6b3e09c0a4203534ef4))
* **UserAnalysisApiKeyService:** do not instance new HttpEntity and ignore deprecated attributes ([0d42a4c](https://github.com/b-partners/bpartners-api/commit/0d42a4c968224fb1c0e8bfad169c4ea08e8de1e8))
* **UserRepository:** handle StripException when retrieving user payment methods ([90584db](https://github.com/b-partners/bpartners-api/commit/90584db0e58b5a82da87b4e912ccf2131dd13d86))
* **UserRestMapper:** check subscription validity before free trial ([7c5e1ae](https://github.com/b-partners/bpartners-api/commit/7c5e1aeb4b4c570b1578d7040eccc405dce4e69b))


### Features

* add airbus pneo source ([675463c](https://github.com/b-partners/bpartners-api/commit/675463c844184e727651356346c901c4dbacca81))
* api key revocation ([090b988](https://github.com/b-partners/bpartners-api/commit/090b988015ce1b19eb62509c535867b9e7384ecc))
* get user analysis api key through existing api keys endpoint ([ec5055e](https://github.com/b-partners/bpartners-api/commit/ec5055ed29cf92248e16f3e181b30bcf9b314efe))
* request analysis api key after user onboarding ([4edc1af](https://github.com/b-partners/bpartners-api/commit/4edc1aff3de8d14125be8284a1df571a386bc6e9))
* support quebec coordinates ([181a374](https://github.com/b-partners/bpartners-api/commit/181a374609783b3fa50216b6d20057831e2a7150))


### Reverts

* Revert "chore: add tech email as bcc on customer crupdate event triggered" ([732b2c4](https://github.com/b-partners/bpartners-api/commit/732b2c4124790af830248ea750ebc0b00ddd493a))



# [0.76.0](https://github.com/b-partners/bpartners-api/compare/v0.75.0...v0.76.0) (2026-01-09)


### Bug Fixes

* **area-picture:** set zoom level to BUILDING(19) when ign the image source ([a750542](https://github.com/b-partners/bpartners-api/commit/a7505421d82d23cdb26572a5dd608ca3eaa074f1))
* handle attachment on prospect notification as binary file ([f4da301](https://github.com/b-partners/bpartners-api/commit/f4da3015216b3a9d1f5880ad7e6ec03be5a95e41))
* **SubscriptionService:** handle not_found stripe customer when retrieving stripe subscriptions ([8ebc681](https://github.com/b-partners/bpartners-api/commit/8ebc68137f64fbe9bb127dad22e58d302f06c718))
* use multipart-file instead of binary on prospect notification ([f7a8298](https://github.com/b-partners/bpartners-api/commit/f7a82981ba8981e3f22785c4c7331826b64faf46))
* **UserController:** add missing @RequestBody annotation on redirectionStatusUrls ([e66d704](https://github.com/b-partners/bpartners-api/commit/e66d704d36e7067e36d102bdf351dd98fbc8eb26))


### Features

* notify prospects creation with attachment ([18ac453](https://github.com/b-partners/bpartners-api/commit/18ac453c7aedc19ec668afad63a116937ec31fa2))
* use POST /prospects/*/accountHolders for creation and PUT for save ([08c03f5](https://github.com/b-partners/bpartners-api/commit/08c03f584d6ecee07a35f8cdcc34503a89eca513))


### Reverts

* Revert "test(debug): commment WhoamiIT and SubscriptionServiceIT" ([d9519e7](https://github.com/b-partners/bpartners-api/commit/d9519e7dd6a2e0c281ebfbb17c3d5a3ba1fdd83d))



# [0.75.0](https://github.com/b-partners/bpartners-api/compare/v0.74.0...v0.75.0) (2025-12-17)


### Bug Fixes

* handle UNPAID status on stripe ([a622f13](https://github.com/b-partners/bpartners-api/commit/a622f138236cbfc25c59cb614dc69e639d637427))
* handle user whitelisted for prospect update ([a292a53](https://github.com/b-partners/bpartners-api/commit/a292a535c633ce615b343b516de2c69946aa0284))
* notify account holder new prospects ([c81011c](https://github.com/b-partners/bpartners-api/commit/c81011c2a1b3e03af39864ba2ea081cf77c2c382))
* prospect must have unique mail ([4624f3d](https://github.com/b-partners/bpartners-api/commit/4624f3ded56ba5fe6341d9fc9f5ef216fdb3dcae))
* **ProspectService:** reverse condition when prospect is new ([bb8fddb](https://github.com/b-partners/bpartners-api/commit/bb8fddb4bf55b23ea7e79ed9cb219d90309c3932))
* retrieve roof analysis subscription from detection tracking ([30f6314](https://github.com/b-partners/bpartners-api/commit/30f6314fc3bfba25b786fe832ab30e56aa2a322e))
* **Subscription:** handle Exception when UNPAID subscription found ([176a533](https://github.com/b-partners/bpartners-api/commit/176a533d62efdf989f8ba158a84c266283b92140))
* **SubscriptionService:** handle active scheduled subscription as domain active subscription ([c4ad0cc](https://github.com/b-partners/bpartners-api/commit/c4ad0ccd1a030a23a3fd311e1adbd3fd5b2cc9f6))


### Features

* access to user billing portal ([8b2da38](https://github.com/b-partners/bpartners-api/commit/8b2da383504a1ac2230b9ade95f496f35fdb2c27))



# [0.74.0](https://github.com/b-partners/bpartners-api/compare/v0.73.1...v0.74.0) (2025-12-05)


### Bug Fixes

* deployment without test ([5de528c](https://github.com/b-partners/bpartners-api/commit/5de528c2eb1723aadc309309e2f899a6220c360d))


### Features

* convert area picture annotation pixel to latlon ([36119d1](https://github.com/b-partners/bpartners-api/commit/36119d1f84cd1bb8ab4dde3e30addabeba2edd8c))



## [0.73.1](https://github.com/b-partners/bpartners-api/compare/v0.73.0...v0.73.1) (2025-12-02)


### Bug Fixes

* sync release version & publish client ([883dbc9](https://github.com/b-partners/bpartners-api/commit/883dbc9339dddceb73c372ae2df83327f74af22d))



# [0.73.0](https://github.com/b-partners/bpartners-api/compare/v0.71.0...v0.73.0) (2025-12-02)


### Bug Fixes

* /catpcha/token SecurityConf ([6149939](https://github.com/b-partners/bpartners-api/commit/6149939c49cbd25361eb46269e0f9293c9f4c8db))
* allow GET users by ID for ADMIN_ROLE ([9843f27](https://github.com/b-partners/bpartners-api/commit/9843f27c15980e0e9c3b219f5dad54741c1e349d))
* fix export area picture annotation data ([49bbf22](https://github.com/b-partners/bpartners-api/commit/49bbf22195fc51a23a2ab588343b38721f185514))


### Features

* expot area picture annotation with annotator 3d ([a627df3](https://github.com/b-partners/bpartners-api/commit/a627df37f635edf843d646a65f53335a5acb79a3))
* implement captcha token verification ([f05ac18](https://github.com/b-partners/bpartners-api/commit/f05ac180dda4a8f47bec1d0c8472272b235335da))



# [0.71.0](https://github.com/b-partners/bpartners-api/compare/v0.70.1...v0.71.0) (2025-10-24)


### Bug Fixes

* get areaPictureMapLayers ([11b619a](https://github.com/b-partners/bpartners-api/commit/11b619a3e679a0dcf5c6fe492115efae5adabbe3))
* **ProspectJpaRepository:** filter by old_name or new_name not only old_name ([a82203c](https://github.com/b-partners/bpartners-api/commit/a82203c6e86cd47eb9dbe371ce8a1c47697e26e6))
* **Prospect:** persist creation datetime and order list by creation datetime DESC ([2fe4cdf](https://github.com/b-partners/bpartners-api/commit/2fe4cdfc7c0534e5aa86847bc2f120666b4e58ed))
* **Prospect:** persist update datetime and order list by update datetime DESC ([24fdd94](https://github.com/b-partners/bpartners-api/commit/24fdd94d4962ca99d69da306963ef30e12175d56))
* **Prospect:** remove sort by lastEvaluationDate ([69dba1a](https://github.com/b-partners/bpartners-api/commit/69dba1a3a7fd102db6c7ec4a95a3e2537757de1c))
* **Prospect:** sort list with nulls last ([4615c94](https://github.com/b-partners/bpartners-api/commit/4615c944c41d519b384b4b8bab8837a021475a99))
* remove year from hauts-de-seine departement name ([d64d5ed](https://github.com/b-partners/bpartners-api/commit/d64d5edf131243fb923d58f99581e4c423336ff8))
* update metz layer and exclude an user from RoofAnalysisConsumptionFreeTrialValidator ([849ba7f](https://github.com/b-partners/bpartners-api/commit/849ba7f475cba8a7f2bb436f75423a0144acd5e9))


### Features

* area picture annotation converter ([0fb3f40](https://github.com/b-partners/bpartners-api/commit/0fb3f4020bd11c31514d0c845ba69129bb430f5a))
* GET /users/{id}/keys for ADMIN_ROLE ([2051cad](https://github.com/b-partners/bpartners-api/commit/2051cad759984f32c72c2d4af0b3c54ed9ed8a9f))



## [0.70.1](https://github.com/b-partners/bpartners-api/compare/v0.70.0...v0.70.1) (2025-10-14)


### Bug Fixes

* export pdf ([51cfb42](https://github.com/b-partners/bpartners-api/commit/51cfb4238d81fb7560926e0a6be637f522b605c3))
* **ExportAreaPictureAnnotation:** return presignedURL instead of byte directly ([ff1baa2](https://github.com/b-partners/bpartners-api/commit/ff1baa25ccb97a48570eadf2c7a1579a30212cf3))



