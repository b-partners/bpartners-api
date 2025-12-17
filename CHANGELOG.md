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



# [0.70.0](https://github.com/b-partners/bpartners-api/compare/v0.68.0...v0.70.0) (2025-10-13)


### Bug Fixes

* do not add SelfMatcher for POST /users/*/keys ([5a67c49](https://github.com/b-partners/bpartners-api/commit/5a67c49b8a9d608d6ac4ef8de3fab31e7c851faf))
* do not add SelfUserMatcher for POST /users/*/keys ([50a0aee](https://github.com/b-partners/bpartners-api/commit/50a0aee9261155bf6f0f27e7e4c5bf6b91d03a95))
* do not use invoice status list in request param ([144993c](https://github.com/b-partners/bpartners-api/commit/144993c27e63c3c7ba723ba5db1b3f1ed074faa2))
* http method should be post instead of put on landing file retrieval ([0ce09f4](https://github.com/b-partners/bpartners-api/commit/0ce09f4b53c7896657507bf819460e7bc501b487))
* implement add 501 and 503 response on crupdateAreaPictureDetails ([7721a61](https://github.com/b-partners/bpartners-api/commit/7721a613225e3ced531d5b381afe79af5cafe29a))
* **Invoice:** compute payment regulation event if invoiceStatus=CONFIRMED ([4fd5c82](https://github.com/b-partners/bpartners-api/commit/4fd5c82e375b0a919fd4041e7a470a2bdf58db43))
* release version ([d45c90f](https://github.com/b-partners/bpartners-api/commit/d45c90f574406a09f458088c0909d919ae334c2b))
* use url encoded comma format ([748b0d9](https://github.com/b-partners/bpartners-api/commit/748b0d9753de0a0c17f04c018f9242723c9fd85f))


### Features

* use lambda-url ([305f8e2](https://github.com/b-partners/bpartners-api/commit/305f8e234a14724565406c1cc8719b36df3b808f))


### Reverts

* chore: remove bridge api ([d09a34c](https://github.com/b-partners/bpartners-api/commit/d09a34c7ceb884983c9fb2228aab8af4c6f0d380))



# [0.68.0](https://github.com/b-partners/bpartners-api/compare/v0.67.0...v0.68.0) (2025-07-18)


### Bug Fixes

* check bucket conf during url presigning ([2b01d89](https://github.com/b-partners/bpartners-api/commit/2b01d89572f4117e49a7c15f2d563bad0ccf47a2))
* **FintecturePaymentInfoRepository:** filter payment by date_from today minus 1 day ([4319b0b](https://github.com/b-partners/bpartners-api/commit/4319b0b81c3f64b7d5521c53cf536762f6fd9388))
* handle cancelled reneweal ([68c26e3](https://github.com/b-partners/bpartners-api/commit/68c26e32f70d7582979d2adc5fd1549bc0bf697f))
* retrieve validated userApiKeyFullAuthorization from specific persisted table ([e580d9d](https://github.com/b-partners/bpartners-api/commit/e580d9ddf66d489bf65f0a620ec0f0c73fa84cab))
* **RoofAnalysisConsumptionFreeTrialValidator:** do not filter consumption for user with apiKey ([7b09503](https://github.com/b-partners/bpartners-api/commit/7b095039922aa9180a3a1a00b101d4c7ef501146))
* set default invoice.delayInPaymentAllowed=7 days ([4095082](https://github.com/b-partners/bpartners-api/commit/40950824184c483f198af726696a7de48d095e24))
* throw exception when provided apiKey null during update ([b5d78e6](https://github.com/b-partners/bpartners-api/commit/b5d78e6855b4cc7a161e70fc55e35cea3c2a6714))
* **UsernamePasswordAuthenticatorFacade:** do not validate subscription for user with apiKeyF ullAuthorization ([a48fdf0](https://github.com/b-partners/bpartners-api/commit/a48fdf066d0e587005b13388c3bdc6b55026bd7a))


### Features

* GET /areaPictureMapLayers providing longitude and latitude ([1f355cf](https://github.com/b-partners/bpartners-api/commit/1f355cf2158f30f84dd4aefc43ef4b72fd331ada))
* GET /users by criteria for ADMIN_ROLE ([f963e54](https://github.com/b-partners/bpartners-api/commit/f963e54d5dfad4bcbe128c9761b99337911d7bfd))
* retrieve user api key by token ([9c1c570](https://github.com/b-partners/bpartners-api/commit/9c1c5707c010721a317d77c2ac073a0f8db2784f))
* update user api key by ADMIN_ROLE ([d95e03e](https://github.com/b-partners/bpartners-api/commit/d95e03e4cd6b3d07185167d14cdcdbf9a3f142b0))
* upload and retrieve from landing bucket ([a49c962](https://github.com/b-partners/bpartners-api/commit/a49c96299b21a2062dc65272c10c93a3232f1da3))



# [0.67.0](https://github.com/b-partners/bpartners-api/compare/v0.66.0...v0.67.0) (2025-04-24)


### Features

* allow client provide autocompletion sessionId ([da8390e](https://github.com/b-partners/bpartners-api/commit/da8390e2e15fe12f534bb7f0ad93c3a49267e305))



# [0.66.0](https://github.com/b-partners/bpartners-api/compare/v0.65.0...v0.66.0) (2025-04-24)


### Bug Fixes

* add onboarded user to admin customers ([3e16cd8](https://github.com/b-partners/bpartners-api/commit/3e16cd8fe80267467f2a0c86798ba3f789c639e2))
* do not filter by date users to compute invoice ([7fdad2c](https://github.com/b-partners/bpartners-api/commit/7fdad2cf46f48d55f1eaf8e32c2d4ffc6996fa39))
* **MonhtlySubscriptionInvoiceRequestedService:** toPayAt = fifthOfNextMonth ([e890b53](https://github.com/b-partners/bpartners-api/commit/e890b53637b0beb99675d30c6b7bb35282d701c8))
* **MonthlySubscriptionInvoiceRequestedService:** set default subscription period and sendingDate ([a1bcff5](https://github.com/b-partners/bpartners-api/commit/a1bcff5d888b8924c685e9db2fe75b084e704848))
* PUT /accounts/*/areaPictures/* handles null prospectId ([c2db0ac](https://github.com/b-partners/bpartners-api/commit/c2db0ac6aa805a69ae76a452b137f4494bac03fa))
* **SubscriptionController:** remove POST /users/id/subscriptionConsumptionLogs implementation ([82df2ff](https://github.com/b-partners/bpartners-api/commit/82df2ffbf245fa1de0c578a18628c23ab623442c))


### Features

* POST /users/id/detectionTracking without consumption log computed ([dfb6b0c](https://github.com/b-partners/bpartners-api/commit/dfb6b0cde56ca7e339c881867cead7fac85698b0))
* provide autocompletion when user typing address ([ba2ca76](https://github.com/b-partners/bpartners-api/commit/ba2ca763a1c898ad5f95744d4cf07d036d9c8ef1))



