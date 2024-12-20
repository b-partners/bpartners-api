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



## [0.54.1](https://github.com/b-partners/bpartners-api/compare/v0.54.0...v0.54.1) (2024-09-10)


### Bug Fixes

* avoid NPE on prospect getById ([2bd849c](https://github.com/b-partners/bpartners-api/commit/2bd849c0710e55691252fd345a48453236d57149))
* paginate getProspects ([edf00da](https://github.com/b-partners/bpartners-api/commit/edf00da1858245dda8d58e0946b64612e4f66c5f))



# [0.54.0](https://github.com/b-partners/bpartners-api/compare/v0.52.1...v0.54.0) (2024-09-10)


### Bug Fixes

* do not delete old invoice.paymentRequests after update ([acaedbb](https://github.com/b-partners/bpartners-api/commit/acaedbb249014a844c82e1708bcd00ce583082f4))


### Features

* paginate getProspects and add get prospect by id ([c0f8a9e](https://github.com/b-partners/bpartners-api/commit/c0f8a9eb43ea3d2b5ceabdbf9849b3b9d5a85e1e))


### Reverts

* Revert "chore(to-revert): allow invoice duplication for userRole=EVAL_PROSPECT" ([d2eec6d](https://github.com/b-partners/bpartners-api/commit/d2eec6d56b8030349c2e6a50eb7df741d94e367d))



## [0.52.1](https://github.com/b-partners/bpartners-api/compare/v0.52.0...v0.52.1) (2024-09-09)


### Bug Fixes

* update payment regulations after updating invoice payment request ([8b901a7](https://github.com/b-partners/bpartners-api/commit/8b901a783719311deed214f56c6e789fa0eaeeee))



# [0.52.0](https://github.com/b-partners/bpartners-api/compare/v0.50.1...v0.52.0) (2024-09-05)


### Bug Fixes

* handle concurrent invoice crupdate ([6f5fc9c](https://github.com/b-partners/bpartners-api/commit/6f5fc9c6dc775855fdaa133de90af3bc5c6539c1))
* retrieve bbox with the exact values ([5d8989d](https://github.com/b-partners/bpartners-api/commit/5d8989dcd5117925f152ac34433b3b5b0749006a))
* use access token instead of bearer when deleting bridge item ([46b0bb1](https://github.com/b-partners/bpartners-api/commit/46b0bb16fdefa3c9b04ab86c47028a2c956ebe87))


### Features

* add ign geoserver image source ([24beca7](https://github.com/b-partners/bpartners-api/commit/24beca77ba856949644f82b411324b2b8ef507c9))
* add isDraft on AreaPictureAnnotation ([b4e8390](https://github.com/b-partners/bpartners-api/commit/b4e839085e9871b7abd72111812339cb36c53b38))
* change get draft annotation response ([d8accec](https://github.com/b-partners/bpartners-api/commit/d8accec77fcdb1892c7f213957797ed8843429f4))
* get draft annotations by accountId ([842b192](https://github.com/b-partners/bpartners-api/commit/842b192b5b426d93221dd720cea50bfc5c39f831))



## [0.50.1](https://github.com/b-partners/bpartners-api/compare/v0.50.0...v0.50.1) (2024-07-05)


### Bug Fixes

* add CREDIT CARD payment method ([572e3e1](https://github.com/b-partners/bpartners-api/commit/572e3e1d98db0b66892a72198cdb64f6c2aa11dc))
* add firstName to prospectJpaRepository.findAllByStatus native query ([6db59df](https://github.com/b-partners/bpartners-api/commit/6db59dfada41f9cccc8bd0b609533b428894f699))
* permitAll on poja endpoints ([86bd23c](https://github.com/b-partners/bpartners-api/commit/86bd23c29e99130cf8f97da7c2738d7bf8d8d63c))



