## [0.69.1](https://github.com/b-partners/bpartners-api/compare/v0.69.0...v0.69.1) (2025-10-06)


### Bug Fixes

* release version ([5be2edb](https://github.com/b-partners/bpartners-api/commit/5be2edb0dc2b0b370e88baf159ae0b73c5521386))



# [0.69.0](https://github.com/b-partners/bpartners-api/compare/v0.68.0...v0.69.0) (2025-10-03)


### Bug Fixes

* do not add SelfMatcher for POST /users/*/keys ([5a67c49](https://github.com/b-partners/bpartners-api/commit/5a67c49b8a9d608d6ac4ef8de3fab31e7c851faf))
* do not add SelfUserMatcher for POST /users/*/keys ([50a0aee](https://github.com/b-partners/bpartners-api/commit/50a0aee9261155bf6f0f27e7e4c5bf6b91d03a95))
* do not use invoice status list in request param ([144993c](https://github.com/b-partners/bpartners-api/commit/144993c27e63c3c7ba723ba5db1b3f1ed074faa2))
* http method should be post instead of put on landing file retrieval ([0ce09f4](https://github.com/b-partners/bpartners-api/commit/0ce09f4b53c7896657507bf819460e7bc501b487))
* implement add 501 and 503 response on crupdateAreaPictureDetails ([7721a61](https://github.com/b-partners/bpartners-api/commit/7721a613225e3ced531d5b381afe79af5cafe29a))
* **Invoice:** compute payment regulation event if invoiceStatus=CONFIRMED ([4fd5c82](https://github.com/b-partners/bpartners-api/commit/4fd5c82e375b0a919fd4041e7a470a2bdf58db43))
* use url encoded comma format ([748b0d9](https://github.com/b-partners/bpartners-api/commit/748b0d9753de0a0c17f04c018f9242723c9fd85f))


### Features

* use lambda-url ([305f8e2](https://github.com/b-partners/bpartners-api/commit/305f8e234a14724565406c1cc8719b36df3b808f))



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



# [0.65.0](https://github.com/b-partners/bpartners-api/compare/v0.64.0...v0.65.0) (2025-04-07)


### Bug Fixes

* **InvoicePDFProcessor:** allow processing without logo ([c413899](https://github.com/b-partners/bpartners-api/commit/c4138996fd761b2c114cc505d8a9a7fc59c8cab1))
* **MonthlySubscriptionInvoiceRequestedService:** compute invoice for actual month date not last month ([1930d4b](https://github.com/b-partners/bpartners-api/commit/1930d4b530db6c607773fb77d4e9c0a74c3d39be))
* pcrs resolution ([9596184](https://github.com/b-partners/bpartners-api/commit/95961844a2fb73a2f28a34a12d175cbdcde0d61a))
* remove unnecessary log on GeoCodingApiTest ([264e09d](https://github.com/b-partners/bpartners-api/commit/264e09d62628b394eaf0f31ccfbd51283f67798e))
* set AreaPictureConsumptionValidator with max free roof analysis consumption ([78c2742](https://github.com/b-partners/bpartners-api/commit/78c2742a8d099742ef3031b2d418b4ae46ec2d5d))
* whoami with api key ([8d4ddec](https://github.com/b-partners/bpartners-api/commit/8d4ddec11bfd88d8028af9f6dc4575f0799bed76))


### Features

* add susbscriptionConsumptionLog endpoint ([5769426](https://github.com/b-partners/bpartners-api/commit/576942662d25c568fc50583c4406da12f27a4fc1))
* get subordinates users of an user ([1ba3b10](https://github.com/b-partners/bpartners-api/commit/1ba3b101ed3bcb6c168cae44c736837deb7e03f7))
* make area picture creation permit for unauthenticated user ([59fae2d](https://github.com/b-partners/bpartners-api/commit/59fae2dc1d6e72cb3f82f0f458f2616e1e0a7f18))


### Reverts

* Revert "refactor: add price if trial is use" ([e0e5b2a](https://github.com/b-partners/bpartners-api/commit/e0e5b2aa42f04a9a6a08c0cac60b83b1b4125098))
* Revert "chore(to-fix): WhoamiIT use TestUtils::restJoeDoeUser" ([defca1d](https://github.com/b-partners/bpartners-api/commit/defca1d375d63813ec5ddb4d552f7f614c10374a))
* Revert "feat: make area picture creation permit for unauthenticated user" ([59f8827](https://github.com/b-partners/bpartners-api/commit/59f882722030fbb224d7e165b54b48bb21b1a5c0))
* Revert "chore(to-revert): comment deactivate fintecture" ([0e21db6](https://github.com/b-partners/bpartners-api/commit/0e21db6df388fb9f4b5f913fb058a152a0c3c3ab))
* chore(to-revert): get invoice for february 2025 ([e18abbf](https://github.com/b-partners/bpartners-api/commit/e18abbfd60b2a61c1664c65bccaecb3e6edab291))



# [0.64.0](https://github.com/b-partners/bpartners-api/compare/v0.63.0...v0.64.0) (2025-03-11)


### Reverts

* Revert "feat: make area picture creation permit for unauthenticated user" ([1e4ca90](https://github.com/b-partners/bpartners-api/commit/1e4ca90fe4d6bf7460a5309a7709406e6d4d3495))



# [0.63.0](https://github.com/b-partners/bpartners-api/compare/v0.62.0...v0.63.0) (2025-03-10)


### Bug Fixes

* add pcrs and aerial photography layer in default layers ([8b07aaa](https://github.com/b-partners/bpartners-api/commit/8b07aaaf84ee43665dcd82a4e70ce2840f577858))
* export annotation image generator measurementCoordinate ([58e281b](https://github.com/b-partners/bpartners-api/commit/58e281b9b5b2ca68c4a2c8a169cba7f119f3b19b))
* **MonthlySubscriptionInvoiceRequestedService:** compute invoice for actual month date not last month ([9fb3bd4](https://github.com/b-partners/bpartners-api/commit/9fb3bd494b7750c3921fcc7185fc266711d85fe4))
* pcrs resolution ([e5290e0](https://github.com/b-partners/bpartners-api/commit/e5290e009a63eef50c0fe38be5899055c975dac4))
* remove unnecessary log on GeoCodingApiTest ([1f00fe9](https://github.com/b-partners/bpartners-api/commit/1f00fe9dc69c644c78cb75aee09d3da1fb0a3876))


### Features

* export annotation generate image ([3b1ecb8](https://github.com/b-partners/bpartners-api/commit/3b1ecb8572f7861bf64052e498750f8beac03ccf))
* export area picture annotation endpoint ([4076ad4](https://github.com/b-partners/bpartners-api/commit/4076ad4e30787a8ab7a8c5b56d6725f0b883354f))
* export area picture annotation pdf service ([e5214d9](https://github.com/b-partners/bpartners-api/commit/e5214d9b7f3e5ba5a2735ecc5ab6c5f621f65414))
* get subordinates users of an user ([e0fad8b](https://github.com/b-partners/bpartners-api/commit/e0fad8b7dccd4921d4b7dda05a9b2cb3db11c566))
* make area picture creation permit for unauthenticated user ([fe3c290](https://github.com/b-partners/bpartners-api/commit/fe3c2904ada1dcac6a7c14823e6630699c219896))



# [0.62.0](https://github.com/b-partners/bpartners-api/compare/v0.61.0...v0.62.0) (2025-01-31)


### Bug Fixes

* add variable analysis product into monthly subscription according to consumption ([f9554e6](https://github.com/b-partners/bpartners-api/commit/f9554e6ce2956a57d37e8d047f0655ee906b7c27))
* allow free_trial user to subscribe ([e9ce607](https://github.com/b-partners/bpartners-api/commit/e9ce607c866ce24463f5274f94d351c7d74e63c8))
* get stripe susbcription product by specific request when retrieving from item ([6aad791](https://github.com/b-partners/bpartners-api/commit/6aad79132fd180cba50cee556f04b9befcd2e407))
* rename subscription status CANCELLED to CANCELED ([6f5cb5c](https://github.com/b-partners/bpartners-api/commit/6f5cb5cb3a1d1bfd939d08711e32e9d48b39d731))
* SecurityConf.subscriptionConsumptionLogs filter from SelfUserMatcher ([a2e45c3](https://github.com/b-partners/bpartners-api/commit/a2e45c352ba1ae41f76f90d17b54c7002caacdd8))
* set active subscription event TRIALING status ([7975bf8](https://github.com/b-partners/bpartners-api/commit/7975bf8500e908db60f7e7238414a98f029f8137))
* show zip code and city in PDF invoice ([1b9f8f9](https://github.com/b-partners/bpartners-api/commit/1b9f8f97fec50234bcc8e08181f48b59ed2dc74d))
* subscription with variable consumption logs for active subscription ([6493838](https://github.com/b-partners/bpartners-api/commit/64938386211286ff6407848145981339d93a95eb))


### Features

* add pcrs and aerial photography layer ([a792348](https://github.com/b-partners/bpartners-api/commit/a7923484bdb530891c67d6f3d268e3ee192f85c4))
* get consumption logs ([83ff43b](https://github.com/b-partners/bpartners-api/commit/83ff43b59af5a39f94bcd54788672a9166559370))
* monitor usage consumption and compute usage record ([0761dc7](https://github.com/b-partners/bpartners-api/commit/0761dc70adba0caa986977bb3c0fcb663f14b073))



# [0.61.0](https://github.com/b-partners/bpartners-api/compare/v0.60.0...v0.61.0) (2025-01-10)


### Bug Fixes

* do not debit users before fifth of next months ([2c97d69](https://github.com/b-partners/bpartners-api/commit/2c97d69de68e135d94d272cdbb9b3fc2a9ac6461))
* do not require subscription for 14 trial period days ([89d1710](https://github.com/b-partners/bpartners-api/commit/89d17101da3dfb4159d24874a4ab46416f87af2a))


### Features

* **api:** handle FREE_TRIAL subscription status for user ([c94954d](https://github.com/b-partners/bpartners-api/commit/c94954d7dfb0e20f2451a95d9401d66074985f12))



