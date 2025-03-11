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



# [0.60.0](https://github.com/b-partners/bpartners-api/compare/v0.59.0...v0.60.0) (2025-01-08)


### Bug Fixes

* add controller registerActiveUsersWithNullSubscription ([9301efe](https://github.com/b-partners/bpartners-api/commit/9301efec1bde5a1cb1f5aceaa22ec2f7edf57b59))
* add incremental nb to invoice reference for subscription ([e82c46e](https://github.com/b-partners/bpartners-api/commit/e82c46e9fc678cd5949070168fcba378c5b45401))
* allow not subscribed user to initiate subscription ([c89dc10](https://github.com/b-partners/bpartners-api/commit/c89dc101dc8e762a468e527813d7a52a333f672b))
* begin offset from 0 when filtering users by criteria ([f377e4a](https://github.com/b-partners/bpartners-api/commit/f377e4a10c2362da406c1a17d74b3abd94f372cc))
* comput vat and total price before generating subscription invoice ([b97f195](https://github.com/b-partners/bpartners-api/commit/b97f195614e537a360fb25dac6ac0e2288fca9bc))
* create customer for userToCredit when not found ([158381d](https://github.com/b-partners/bpartners-api/commit/158381d1d2feb91dfd9d44b327a6db82e3ce6e38))
* create or link user subscription when not existing ([928e62a](https://github.com/b-partners/bpartners-api/commit/928e62a3dfd9f8c0a98dd212578030e182dc8c72))
* do not compute invoice for user without subscription active ([7c8e963](https://github.com/b-partners/bpartners-api/commit/7c8e9634b5668db1bed27aa7f7c7b1bb0ea7c24e))
* do not override invoice.toPayAt in case of susbcription invoice ([b568756](https://github.com/b-partners/bpartners-api/commit/b568756323719a476fccc5784128c3e1a68bd69e))
* filter db user with missing susbscription by e2Id ([c3ff526](https://github.com/b-partners/bpartners-api/commit/c3ff5267fd219ad63c5d318a81e7057c96e0d399))
* generate reference for susbcription invoice triggered ([08d68dd](https://github.com/b-partners/bpartners-api/commit/08d68ddef7e44c6d917c7a14c062892f0f06ff2f))
* hide paymentUrl caption in PDF invoice when null ([017ae5a](https://github.com/b-partners/bpartners-api/commit/017ae5a757772421441014d87669249e8a529342))
* make onboarded user eligible to subscription check ([63fb469](https://github.com/b-partners/bpartners-api/commit/63fb4694c7be86748c731d257818e4984be3c1ec))
* set customer names as user to debit in invoice monthly susbcription ([5288425](https://github.com/b-partners/bpartners-api/commit/5288425603a3b32309f26938d96c231a2e02cb32))
* set user page as int attribute in MonthlySubscriptionInvoiceRequested ([901e1d6](https://github.com/b-partners/bpartners-api/commit/901e1d6773b737270f899f5ca5dc512261407edc))
* trigger user registration requested for all null subscription ([6c435c9](https://github.com/b-partners/bpartners-api/commit/6c435c90142b04fb097e6cb0f97fd1b88f98793e))
* update email infos for subscription invoice ([ed9f354](https://github.com/b-partners/bpartners-api/commit/ed9f3547e6bd91f4c5d53831e15b2d75c2a4c794))


### Features

* add direct-debit payment method for invoice ([7f4f9ad](https://github.com/b-partners/bpartners-api/commit/7f4f9adb2a3bd3f3c7eebeb3c3162443f05364e1))
* trigger monthly subscription invoice ([6d2178e](https://github.com/b-partners/bpartners-api/commit/6d2178eec902e81fc3d4b0ae835e4e13943ad6da))



# [0.59.0](https://github.com/b-partners/bpartners-api/compare/v0.58.0...v0.59.0) (2024-12-09)


### Features

* add humidityLevel on annotationInstance ([1ef58fc](https://github.com/b-partners/bpartners-api/commit/1ef58fcaea5c495ceceb0f3f11d7d0dcfca7fadc))
* cancel user subscription ([aa56e18](https://github.com/b-partners/bpartners-api/commit/aa56e18818aa0ffb632ec93fb928b5794a0b2747))



# [0.58.0](https://github.com/b-partners/bpartners-api/compare/v0.57.0...v0.58.0) (2024-11-28)


### Bug Fixes

* do not authorize subscription if stripe customer not associated ([2ecab7b](https://github.com/b-partners/bpartners-api/commit/2ecab7bb31a4908d84d8b39c18f347d32f8d7b3e))
* empty subscription is considered as invalid ([521a5a2](https://github.com/b-partners/bpartners-api/commit/521a5a2e21207a7f9e24fe9c24008a6db3dba1f8))
* subscriptionInitiation endpoint accessible without AuthProvider filter ([508edc8](https://github.com/b-partners/bpartners-api/commit/508edc8cb24c36e523e4d6f4699959c70d42229c))


### Features

* include subscription period in user attributes ([d0820f5](https://github.com/b-partners/bpartners-api/commit/d0820f56bda603b2f2126daf28ae5bc4fd148abd))



# [0.57.0](https://github.com/b-partners/bpartners-api/compare/v0.56.0...v0.57.0) (2024-11-28)


### Bug Fixes

* do not rename invoice file before zipping during export ([d89e053](https://github.com/b-partners/bpartners-api/commit/d89e053708895bbc4b52aec46e26c3eb0a847066))
* export invoice  ([677ab0e](https://github.com/b-partners/bpartners-api/commit/677ab0e4e67cf620a314c871215fdfc24397dc96))
* ignore InvoiceStatus.ACCEPTED in invoices export link filter ([a35312b](https://github.com/b-partners/bpartners-api/commit/a35312b10330f08ad26f12343cee0d4a706170c6))
* only check susbcription for eligible user ([ff5e109](https://github.com/b-partners/bpartners-api/commit/ff5e109b17b997a58dfa74b476167ac1cee89a13))
* prospect status filter ([6b5326e](https://github.com/b-partners/bpartners-api/commit/6b5326e5816949acdaac7f16bada8a4347e4b569))
* recenter image and extension ([7bc432c](https://github.com/b-partners/bpartners-api/commit/7bc432cbb631c6b1e38c06147a421b1b1f5ffd95))
* remove SelfAccountMatcher to invoices exportLink security conf ([3db59ab](https://github.com/b-partners/bpartners-api/commit/3db59ab2679fb2e16860b7113f40348a9986767a))
* rename SubscriptionProduct.e2Id column name ([f4b8462](https://github.com/b-partners/bpartners-api/commit/f4b84629e8675d6fd7b637326ef2112a52fb823b))
* retrieve invoices to export from MIN_PAGE-1 ([a9b95fd](https://github.com/b-partners/bpartners-api/commit/a9b95fd2420d10a774fcd80f071462f3c2d6e834))
* set zipEntry path to randomUUID in FileZipper ([358f869](https://github.com/b-partners/bpartners-api/commit/358f86985550916c5cd357eecf6d3eaa8538fb2c))
* validate user subscription in auth provider ([2dba14b](https://github.com/b-partners/bpartners-api/commit/2dba14bcce80265f368c1727f0c534caecda68c0))


### Features

* add subscription status attribute to rest user ([d0c3a6a](https://github.com/b-partners/bpartners-api/commit/d0c3a6a0707d28d24addd0286d8838954f03a06f))
* generate invoices export link ([043661e](https://github.com/b-partners/bpartners-api/commit/043661e43ae4120832f50e2ab247488abfaf218e))
* implement delete user ([e4d0c66](https://github.com/b-partners/bpartners-api/commit/e4d0c66e54c82525a78b2df00c6c9221d3ca49c5))
* initiate user subscription ([92d2e3d](https://github.com/b-partners/bpartners-api/commit/92d2e3d842988a2847898081d8680078dbbd109d))



# [0.56.0](https://github.com/b-partners/bpartners-api/compare/v0.55.0...v0.56.0) (2024-10-04)


### Features

* add tile extension with left-right switching support ([731e7e5](https://github.com/b-partners/bpartners-api/commit/731e7e532f5eaff269d2f42a9ad1974ecb72fc34))



# [0.55.0](https://github.com/b-partners/bpartners-api/compare/v0.54.1...v0.55.0) (2024-09-19)


### Bug Fixes

* check payment regulations changed before invoice crupdate ([5cdb7ab](https://github.com/b-partners/bpartners-api/commit/5cdb7ab200db333ee26b6a2ed5958d60be9bb067))
* compute payment regulations for CONFIRMED invoice edition directly ([063638f](https://github.com/b-partners/bpartners-api/commit/063638fb2e94e9ba7ab539286b0d09769fa93a62))


### Features

* allow users to send email during onboarding ([f89c650](https://github.com/b-partners/bpartners-api/commit/f89c650514d00037a5f01faebba7d94948940671))



