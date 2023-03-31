# [0.11.0](https://github.com/b-partners/bpartners-api/compare/v0.10.0...v0.11.0) (2023-03-31)


### Bug Fixes

* duplicate V0_133 ([#637](https://github.com/b-partners/bpartners-api/issues/637)) ([bbbe6d5](https://github.com/b-partners/bpartners-api/commit/bbbe6d524da971d809fe1386b07ed28fcfa51da9))
* leave user token as cognito bearer when bridge bearer not retrieved ([8c788ed](https://github.com/b-partners/bpartners-api/commit/8c788ed22008bab1a0036dead9ae2b8e373f389a))
* persist provided user ID during updating persisted account ([e88236a](https://github.com/b-partners/bpartners-api/commit/e88236afc18d076f4b568389ddd07835b4e168c9))
* persisted transactions are updated when null are actual values ([4603d75](https://github.com/b-partners/bpartners-api/commit/4603d75ebb67196b30a90f1ac0f7176da87c13e3))
* properly resolve ssm variables for cognito ([cca423f](https://github.com/b-partners/bpartners-api/commit/cca423f139f6a7a434ae8ca65ece9b3a5d8dad69))
* remove profile arg in cd-compute ([1b09c46](https://github.com/b-partners/bpartners-api/commit/1b09c46c4d9863319ae8186dbbebe646ef97b4aa))
* retrieve user from cognito token in legal file controller ([c5b482f](https://github.com/b-partners/bpartners-api/commit/c5b482fd8165618ea02f03a589389b7ba323db58))
* retrieve user from cognito token in user controller ([fb5e0fa](https://github.com/b-partners/bpartners-api/commit/fb5e0fa788bcd52be18a5dd88162d8b2fb44b25e))
* return empty list when any transaction retrieved ([545832c](https://github.com/b-partners/bpartners-api/commit/545832c12cfb326c25f058752b7188a68eb001e4))


### Features

* add town code to accountholder ([#634](https://github.com/b-partners/bpartners-api/issues/634)) ([634a2df](https://github.com/b-partners/bpartners-api/commit/634a2df914a43fb7eb1d66745d3152069efbd3a1))
* exchange cognito authorization code to cognito token ([0461793](https://github.com/b-partners/bpartners-api/commit/04617932874efb62c9f9e8b7b0467627f8007adc))



# [0.10.0](https://github.com/b-partners/bpartners-api/compare/v0.9.0...v0.10.0) (2023-03-23)


### Bug Fixes

* convert paymentRegulation to empty array when null is provided ([8b5756d](https://github.com/b-partners/bpartners-api/commit/8b5756de3017e52abfbe6b7fea3ca6c03030337f))
* delete blank products and customers ([ec4ba9d](https://github.com/b-partners/bpartners-api/commit/ec4ba9d6fd2cc74ad6082db95d82a615961d5e91))
* remove duplicate customer ([ae7e98b](https://github.com/b-partners/bpartners-api/commit/ae7e98b3e61b30420edc9a0da6eae0e5017f88ac))
* remove duplicated customers in the file ([363763a](https://github.com/b-partners/bpartners-api/commit/363763a74bd2417604043ed5b3c8f5e51bd61fb8))
* remove duplicated product when create product by uploading excel file ([3e9b8e9](https://github.com/b-partners/bpartners-api/commit/3e9b8e995c40e9bb71bfa3d6e709a61e85c50a3d))
* remove unique constraint on primary business activity ([3a7d8da](https://github.com/b-partners/bpartners-api/commit/3a7d8da58c01b3603ef246dde25bbc142860d9d0))
* user domain authentication is based on email ([991bc3b](https://github.com/b-partners/bpartners-api/commit/991bc3ba638dc2c65235d7b06eaa49532d0eaf10))


### Features

* add location to accountholder ([#615](https://github.com/b-partners/bpartners-api/issues/615)) ([36bd6cd](https://github.com/b-partners/bpartners-api/commit/36bd6cd8bb86582d77c390bc7f978934322efaf5))



# [0.9.0](https://github.com/b-partners/bpartners-api/compare/v0.8.2...v0.9.0) (2023-03-16)


### Bug Fixes

* account is not updated when user is not associated ([46d9aa9](https://github.com/b-partners/bpartners-api/commit/46d9aa95d9f7e634849d6f34617138d160e98512))
* add percent value on each invoice payment regulation ([6ded317](https://github.com/b-partners/bpartners-api/commit/6ded317e5f3f04f18fa3ab600776d9aa8603d9c8))
* associate accounts to user during mapping   ([2809561](https://github.com/b-partners/bpartners-api/commit/28095611cbe188e7494ca482a1121d8238c5e202))
* ignore unknown properties with sogefi api ([cae33ee](https://github.com/b-partners/bpartners-api/commit/cae33ee7ddf90f20d551abae1e09ca9a6a01400e))
* payment initiation reference is correctly computed ([2946fc9](https://github.com/b-partners/bpartners-api/commit/2946fc9037558d39190d08e79e04f135b07852ad))
* payments regulations are correctly persisted ([b5edd73](https://github.com/b-partners/bpartners-api/commit/b5edd73482a8521e160cdf79223588a8a52b9c23))
* products are ordered by created date time descending by default ([7ba7c9f](https://github.com/b-partners/bpartners-api/commit/7ba7c9f4ecb0208daa301e2a0e0f269ac0a91798))
* repair minor bugs ([f99fe90](https://github.com/b-partners/bpartners-api/commit/f99fe90829062474b42d8ce01e8d53d948a08f55))
* returns persisted user during whoami if swan token not provided ([095fa77](https://github.com/b-partners/bpartners-api/commit/095fa77abcda848bb06a6b3336de4960eebd9ffe))
* same transaction category type handles multiples transaction type ([b41f1ab](https://github.com/b-partners/bpartners-api/commit/b41f1ab13403704c415e02c7e4af4ed00949ce96))
* show payment regulation in invoice generated pdf ([bb848bb](https://github.com/b-partners/bpartners-api/commit/bb848bbf45225ec0ad9f99913db25350031fd6ae))
* typo in tile_layer and roofer ([585a4b3](https://github.com/b-partners/bpartners-api/commit/585a4b35ee6a63c92a4cc5c5818270669997834f))
* unique invoice payment label is invoice reference ([1812541](https://github.com/b-partners/bpartners-api/commit/181254113264999a07f4a1ed84354a8e4a43f865))
* use invoice real reference in payment initiation ([e25e647](https://github.com/b-partners/bpartners-api/commit/e25e64757a483f4eb1a44c398fa286e4f5bc699c))


### Features

* filter customers by criteria ([37506da](https://github.com/b-partners/bpartners-api/commit/37506da2e0f786731b0d35672b79834411b637fa))
* filter products by criteria ([730a148](https://github.com/b-partners/bpartners-api/commit/730a1481c79608b7fab185297232a049a0c4c7e1))
* prospects are fetched from sogefi buildingpermit ([#510](https://github.com/b-partners/bpartners-api/issues/510)) ([444ecc9](https://github.com/b-partners/bpartners-api/commit/444ecc976937cc1f6a8be3d81944e3312a63349e))



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



