## [0.8.2](https://github.com/b-partners/bpartners-api/compare/v0.8.1...v0.8.2) (2023-03-06)


### Bug Fixes

* do not ignore status in ProductRepo::byIdAccountAndStatus ([3b52cf7](https://github.com/b-partners/bpartners-api/commit/3b52cf7276d0b65873ebde8676cbb2933e6d1060))



## [0.8.1](https://github.com/b-partners/bpartners-api/compare/v0.8.0...v0.8.1) (2023-03-02)


### Bug Fixes

* retrieve persisted user when swan is not provided ([cbd3a5b](https://github.com/b-partners/bpartners-api/commit/cbd3a5bacbcb9d7930d9813fba4b8aaef4e5fdb4))
* set environment to run scheduled workflows for dev and preprod only ([#540](https://github.com/b-partners/bpartners-api/issues/540)) ([b529344](https://github.com/b-partners/bpartners-api/commit/b5293449e8dab6fec3569bf6ce33f6e6445ccb32))



# [0.8.0](https://github.com/b-partners/bpartners-api/compare/v0.7.0...v0.8.0) (2023-02-23)


### Bug Fixes

* ignore missing informations during import from file ([3c30def](https://github.com/b-partners/bpartners-api/commit/3c30deffb4cba6033e34ebee61a6bffbeee07884))
* page and page size is optional and have default values ([108b3d7](https://github.com/b-partners/bpartners-api/commit/108b3d76f1b7958b2aa9c3021aa96bd6fa68df76))
* payment regulation works for every invoice status ([858ea4a](https://github.com/b-partners/bpartners-api/commit/858ea4a183583c9d0924351b6268a87d55839d73))
* payment type is mapped correctly ([081b6ad](https://github.com/b-partners/bpartners-api/commit/081b6ad040de3d0aa902c1dfc862bcecb915a702))
* reference is still available during same invoice update ([123be66](https://github.com/b-partners/bpartners-api/commit/123be66098e30679ea9c4f1bce05fa939021ab1e))
* **to-revert:** unit price is set to zero when null is provided during crupdate products ([630dee6](https://github.com/b-partners/bpartners-api/commit/630dee6efc21cf84bca7dc2a7bc3beab52193ad4))


### Features

* add discount to invoice PDF ([1e0684e](https://github.com/b-partners/bpartners-api/commit/1e0684e4d89bd985c9243438cf5ddbf4538f32bf))
* add global discount to invoice ([75029fa](https://github.com/b-partners/bpartners-api/commit/75029fad7740b87b5df29fee8029858754c36b70))



# [0.7.0](https://github.com/b-partners/bpartners-api/compare/v0.6.0...v0.7.0) (2023-02-15)


### Bug Fixes

* set attachment content as base64 format ([d48d7e8](https://github.com/b-partners/bpartners-api/commit/d48d7e81cc1c217a1aa86e13cb2bc09736a76de5))


### Features

* add product created datetime for account ([3964bd8](https://github.com/b-partners/bpartners-api/commit/3964bd82e4cb4c430830fb5ef4bb0e31e3e3c27b))
* crupdate products ([a71fb1e](https://github.com/b-partners/bpartners-api/commit/a71fb1e545355bfc739fe3c1def4b1b59a41dd30))
* import customers from excel file ([bbf1cbb](https://github.com/b-partners/bpartners-api/commit/bbf1cbb366f6a42b1ba28e7ecafea8a0d20a9c17))
* import products from excel file ([6c742d9](https://github.com/b-partners/bpartners-api/commit/6c742d9cf59e3af0fab275ba2826aef669878852))
* invoice handles multiple payments ([566aea5](https://github.com/b-partners/bpartners-api/commit/566aea59cc14f33d1bd4ac989aa2f97fb8f96283))
* order products by criterias ([9176b78](https://github.com/b-partners/bpartners-api/commit/9176b785a1c58c3d2d285afe151d40cd5b0dcd84))



# [0.6.0](https://github.com/b-partners/bpartners-api/compare/v0.5.0...v0.6.0) (2023-02-09)


### Bug Fixes

* display HT instead of TTC when account holder is not subject to vat ([208521e](https://github.com/b-partners/bpartners-api/commit/208521eec25ed612dd32cb8f79bb27f0bfd3212e))
* format revenue target amount attempted as cent ([e30ca7f](https://github.com/b-partners/bpartners-api/commit/e30ca7f28e98b71268f428db9676c5ed83a34bc1))
* refresh transaction summary is invoked every hour ([d58e966](https://github.com/b-partners/bpartners-api/commit/d58e9663f7ae3db79d1d6e874e611212b8ba5c95))
* set product vat percent to 0 by default ([429293d](https://github.com/b-partners/bpartners-api/commit/429293d1d3817b6fe9cadd7df9903bca36bc6955))
* total without vat is shown in confirmed invoice when account holder not subject to vat ([64c1708](https://github.com/b-partners/bpartners-api/commit/64c1708af43e45bcf53e5bf2332f0ff759ab0473))
* total without vat is shown in draft invoice when account holder not subject to vat ([89a96e1](https://github.com/b-partners/bpartners-api/commit/89a96e1690a9273b10096c4171f0e7975faaf04b))


### Features

* add comment field to customer ([a0e8beb](https://github.com/b-partners/bpartners-api/commit/a0e8beba9b089853e225c46e41e34efd04f75704))



# [0.5.0](https://github.com/b-partners/bpartners-api/compare/v0.4.4...v0.5.0) (2023-02-02)


### Bug Fixes

* add input in Sched Depl compute ([#444](https://github.com/b-partners/bpartners-api/issues/444)) ([a746e1d](https://github.com/b-partners/bpartners-api/commit/a746e1dc6d03496367b0c9b157f4ad37b08371e8))
* attachments are persisted ([a424f5f](https://github.com/b-partners/bpartners-api/commit/a424f5fd5711fa30f9482304735c2da52b09196c))
* condition to run tests in cd-compute ([#446](https://github.com/b-partners/bpartners-api/issues/446)) ([f483ec4](https://github.com/b-partners/bpartners-api/commit/f483ec4df0b622a984ba336f725f2a4f16df1b6b))
* cron to run GHA on Madagascar Timezone ([#443](https://github.com/b-partners/bpartners-api/issues/443)) ([11b321a](https://github.com/b-partners/bpartners-api/commit/11b321a7c1aad2df270e6bd7e3a4adb1e2431ebd))
* event to trigger test step in cd-compute.yml ([#439](https://github.com/b-partners/bpartners-api/issues/439)) ([dee247a](https://github.com/b-partners/bpartners-api/commit/dee247a57782bc3b66beb8c816eccb7c9e465949))
* reference input in workflow_call ([#445](https://github.com/b-partners/bpartners-api/issues/445)) ([9a6ff8f](https://github.com/b-partners/bpartners-api/commit/9a6ff8f0ec00e22d8115a7a8f8def87ee6596dbc))
* run tests when trigger is not 'schedule' and run_test different from false ([#448](https://github.com/b-partners/bpartners-api/issues/448)) ([11380c2](https://github.com/b-partners/bpartners-api/commit/11380c23851a6caad0892c9a8a0de56e30bde0d0))
* Sched Depl compute workflow path ([#438](https://github.com/b-partners/bpartners-api/issues/438)) ([5dfd629](https://github.com/b-partners/bpartners-api/commit/5dfd6299df6760baba1f7f3ece7e5280b5842c9b))
* validity date attribute compatibility is transitional during invoice crudpate ([f649991](https://github.com/b-partners/bpartners-api/commit/f649991be3f068035e6136301eeae261181d70cc))


### Features

* add attachments to mail ([2f94cb0](https://github.com/b-partners/bpartners-api/commit/2f94cb0ec3014e91f5db3f68f9a9b8ed656a9f27))
* add revenue targets to account holder ([dd23285](https://github.com/b-partners/bpartners-api/commit/dd23285596bc3e8e7ade46eaf779d42c5bdbf292))
* handle multiple payments initiation ([b2c5e94](https://github.com/b-partners/bpartners-api/commit/b2c5e94a083b07fd08ad9c4ca4e3f8e99d189e8b))



## [0.4.4](https://github.com/b-partners/bpartners-api/compare/v0.4.3...v0.4.4) (2023-01-26)


### Bug Fixes

* ignore git_push.sh file when publishing the codeartifact client ([#432](https://github.com/b-partners/bpartners-api/issues/432)) ([dba71b2](https://github.com/b-partners/bpartners-api/commit/dba71b23bf1a212fab4c38f45a058ee0f12f952b))



## [0.4.3](https://github.com/b-partners/bpartners-api/compare/v0.4.2...v0.4.3) (2023-01-26)


### Bug Fixes

* command pattern on the content to append to .npmignore ([#431](https://github.com/b-partners/bpartners-api/issues/431)) ([564d2b1](https://github.com/b-partners/bpartners-api/commit/564d2b12104555db0915dfff0b8ecf6faf02e696))



## [0.4.2](https://github.com/b-partners/bpartners-api/compare/v0.4.1...v0.4.2) (2023-01-26)


### Bug Fixes

* echo command to append to .npmignore file ([#430](https://github.com/b-partners/bpartners-api/issues/430)) ([f192f24](https://github.com/b-partners/bpartners-api/commit/f192f2493c883fe9384b37f5e8dca2acbe658815))



## [0.4.1](https://github.com/b-partners/bpartners-api/compare/v0.4.0...v0.4.1) (2023-01-26)


### Bug Fixes

* accept null phone number but validate if not null when creating preUsers ([63f1d93](https://github.com/b-partners/bpartners-api/commit/63f1d9313bd5e6bbf19e16457fcfb0209ffc4b34))
* cpu configurations for ecs ([#427](https://github.com/b-partners/bpartners-api/issues/427)) ([257cd32](https://github.com/b-partners/bpartners-api/commit/257cd326dc112c8f8f807520e6e067630465b9ad))
* delete redundant transactions and associated categories from database ([eb02d9f](https://github.com/b-partners/bpartners-api/commit/eb02d9fa78283e3989a24aa1bafaaaa96a2e032f))
* ecs configurations ([#426](https://github.com/b-partners/bpartners-api/issues/426)) ([0247b79](https://github.com/b-partners/bpartners-api/commit/0247b79149bd03b70977f1c41572d75c9ee78853))
* match invoice sending date and today's date ([466a980](https://github.com/b-partners/bpartners-api/commit/466a980b67da2467e7448358f19f04a1d480c695))
* modify package.json, tsconfig.json and .npmignore before publishing react-client to codearifact ([#423](https://github.com/b-partners/bpartners-api/issues/423)) ([45cb68b](https://github.com/b-partners/bpartners-api/commit/45cb68b1de2700ffc3300ed8acabd3b6955e800b))
* set vat to zero when account holder is not subject to vat ([5f605ff](https://github.com/b-partners/bpartners-api/commit/5f605ff50a289f07128c34a15d519ece58837c20))
* tasksDesiredCount type ([#425](https://github.com/b-partners/bpartners-api/issues/425)) ([7194009](https://github.com/b-partners/bpartners-api/commit/7194009cacf4571f4daf07c64dfe34bc4af93328))
* **to-drop:** toPayAt and validityDate have same values for draft invoice ([1fb0e55](https://github.com/b-partners/bpartners-api/commit/1fb0e552c64e24cb7d891c38f4feb76d6d11e4f1))
* transaction mapper use the provided filtered account ID during mapping ([6129753](https://github.com/b-partners/bpartners-api/commit/612975348f167da1b285d86bd88eb37779612cbe))
* update uses version in get-configuration in cd-compute.yml ([#424](https://github.com/b-partners/bpartners-api/issues/424)) ([ea528ef](https://github.com/b-partners/bpartners-api/commit/ea528efab83060206cdcc0b54e7a3b935e6921ee))



