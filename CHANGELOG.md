## [0.14.1](https://github.com/b-partners/bpartners-api/compare/v0.14.0...v0.14.1) (2023-05-03)


### Bug Fixes

* account holder from business activity is mapped from database ([d43788d](https://github.com/b-partners/bpartners-api/commit/d43788d6d010e0967f683ba44fd6e151c0c05c69))
* account holder revenue target update ([7978229](https://github.com/b-partners/bpartners-api/commit/7978229fa5d975b70ca9e02b818f8cb5d7b40a4a))
* check account bic and bic before payment initiation ([a1a9577](https://github.com/b-partners/bpartners-api/commit/a1a95771b5431828600db5fa26645a9dabe4f969))
* check bridge transaction when trying to save transaction ([ac67c04](https://github.com/b-partners/bpartners-api/commit/ac67c044a8657a9e701c5472223fb5f49706821d))
* delay in payment allowed can not be null for CASH payment type invoice ([d537f8f](https://github.com/b-partners/bpartners-api/commit/d537f8fdeb9113c46f668dfe06d8c4f36c00dc9e))
* disable ProspectService.prospect cron ([bd36333](https://github.com/b-partners/bpartners-api/commit/bd363332c93dd8a2b8d1273b0642382317a89e52))
* get bridge transaction when swan not provided when retrieving by ID ([d2fb1e5](https://github.com/b-partners/bpartners-api/commit/d2fb1e5289b7b3bd548f488e3dc349652e65902f))
* get bridge transactions through dynamic bearer ([6388cc1](https://github.com/b-partners/bpartners-api/commit/6388cc194d6b36ab5297be5e36110080cb5f0bda))
* import customers ([fe0bf1e](https://github.com/b-partners/bpartners-api/commit/fe0bf1e261d2b1a73c839f793788e01e403d3594))
* refresh transactions summary from swan transactions only with user access token ([ec4faed](https://github.com/b-partners/bpartners-api/commit/ec4faed199146eff2d0cedcc780036784a63dbd4))
* return account available balance as cash flow in transaction summary ([3931d57](https://github.com/b-partners/bpartners-api/commit/3931d57466cadadbc4b146a5cabbcd3c367d893d))
* return empty transaction list for users not connected to bridge ([99133fc](https://github.com/b-partners/bpartners-api/commit/99133fc5c8d2682a91f800afaaf6a69a139a1a70))
* set bic as null when iniating bank connection ([08636d9](https://github.com/b-partners/bpartners-api/commit/08636d9d180fbb04427917717374f54728b4f42d))
* set users bank connection refresh into 8 hours and avoid max daily refresh exception ([22c9af8](https://github.com/b-partners/bpartners-api/commit/22c9af82ff5e505b8f66984b30ad1c163bc13ec3))
* throw exception when provided email during onboarding is already used ([9a15651](https://github.com/b-partners/bpartners-api/commit/9a15651b8cbc5773354b237971a0b8eff4e1250c))
* update transaction summary updated instant and display decimal social capital ([fda5f76](https://github.com/b-partners/bpartners-api/commit/fda5f76bf28d09b8aafe6912b4a4e03b898c4624))



# [0.14.0](https://github.com/b-partners/bpartners-api/compare/v0.13.0...v0.14.0) (2023-04-25)


### Bug Fixes

* avoid NPE when fintecture can not initiate payment ([d5d1f4d](https://github.com/b-partners/bpartners-api/commit/d5d1f4db8b373b4a0c8567edaaa222b39b799763))
* avoid NPE when updating payment status ([8db7117](https://github.com/b-partners/bpartners-api/commit/8db7117b1d76a7ead86cfe30980d3b1b8f813d23))
* bank are persisted and map to account ([a8f3c30](https://github.com/b-partners/bpartners-api/commit/a8f3c3002b014496b638c2e1b93fa8447812a5ff))
* bridge account balance is mapped as cents ([ee6ff7d](https://github.com/b-partners/bpartners-api/commit/ee6ff7d3335cbacae8d08534073f22cc1fa2c5e5))
* buildingpermit returns onlytotal because onlytotal is true ([#690](https://github.com/b-partners/bpartners-api/issues/690)) ([85d6fa5](https://github.com/b-partners/bpartners-api/commit/85d6fa54cbf7594a5e7190b5d64c2b4149d21a45))
* ClusterNamePrefix --> NamePrefix ([28ef0ce](https://github.com/b-partners/bpartners-api/commit/28ef0cecfb24c1bb1a6e6a5602f3797434069833))
* customer import handle dynamic cell format inside excel file ([6f81bb0](https://github.com/b-partners/bpartners-api/commit/6f81bb0ea3e2b14df6ef468673b6978d650cf105))
* customer import handle numeric cell ([1cf0aa0](https://github.com/b-partners/bpartners-api/commit/1cf0aa0f8332b8f61580d27c2db18eaa8d6cab89))
* do not update account when getting account through ID ([06e7357](https://github.com/b-partners/bpartners-api/commit/06e7357e6bcb43b1c9a8b93a4b4c2b48ce9a9b41))
* get accountHolders concurrently without fail ([4f2ecd3](https://github.com/b-partners/bpartners-api/commit/4f2ecd30560f68dc268cb0cf03403d7d6e1e72f9))
* invoice delay penalty and penalty percent can be hidden   ([ccd6753](https://github.com/b-partners/bpartners-api/commit/ccd6753fee23c3cf5fcecca383d9df6a6e723cd4))
* parse fraction into BigInteger directly without Long conversion ([2cc7855](https://github.com/b-partners/bpartners-api/commit/2cc7855aa7c009e5dd1ac8264dae257a63c4fe38))
* private-url only triggers cd-compute ([0e8a6d8](https://github.com/b-partners/bpartners-api/commit/0e8a6d872cdd20f72fbcf12de0c9e57e56f2325c))
* return new users info with associated account and account holder after onboarding ([b4eeb48](https://github.com/b-partners/bpartners-api/commit/b4eeb48de8f040407c36f49c03dff2b6416fb442))
* set default payment status to unpaid during initiation ([4421fde](https://github.com/b-partners/bpartners-api/commit/4421fde1df9568088c0259ac77258e07d7fb7168))
* set invoice delay payment default value to 30 ([1b0dfdf](https://github.com/b-partners/bpartners-api/commit/1b0dfdf85a4b7ebcaca43b2deb672ddbe9b30239))
* set onboarding endpoint publicly available ([5e36257](https://github.com/b-partners/bpartners-api/commit/5e36257e2939734d2d003c555dca52d8192ddd56))
* throw exception when trying to initiate payment without bic or iban ([64a1da0](https://github.com/b-partners/bpartners-api/commit/64a1da0d81ec84d3219806883d9ef5e0ce1121ce))
* transactions are ordered by payment datetime desc ([3840805](https://github.com/b-partners/bpartners-api/commit/384080551ee0dd5f10af323d438f826537e6831e))
* treat user upserted event when during event poller ([1d1e41c](https://github.com/b-partners/bpartners-api/commit/1d1e41c43416c28240615c6cd2537201dd795bc4))
* use separate ecs cluster and separate listnerrule ([5d0ae19](https://github.com/b-partners/bpartners-api/commit/5d0ae19b9a243d5d465fb4da474e13b1c51065de))


### Features

* accountholder gets prospects from municipalities within a given prospecting perimeter as distance ([df72f18](https://github.com/b-partners/bpartners-api/commit/df72f18e606e399ad276efb2428681058de7e4bf))
* add town code attribute to prospects and filter accountholder prospects by town code ([#674](https://github.com/b-partners/bpartners-api/issues/674)) ([ff8165d](https://github.com/b-partners/bpartners-api/commit/ff8165d6465e5a0be37270cdb32d8d46643e08e8))
* onboard new user through cognito and bridge ([8bccd7d](https://github.com/b-partners/bpartners-api/commit/8bccd7d93ab2cb21ee5ac4e8ee97b53059e29730))
* send daily email to accountHolders when they’re late on their revenue target ([#610](https://github.com/b-partners/bpartners-api/issues/610)) ([5f7d702](https://github.com/b-partners/bpartners-api/commit/5f7d7023ab77c6c7599228e84456fc12be1d2870))
* use cgu-18-04-23.pdf ([f3b6e1d](https://github.com/b-partners/bpartners-api/commit/f3b6e1d1b92aeba2e850849d04cd4b63f4bd5521))
* user can handle a preferred account ([b09dc95](https://github.com/b-partners/bpartners-api/commit/b09dc95df0feed973d4a9badcd4bd492ecc6896e))



# [0.13.0](https://github.com/b-partners/bpartners-api/compare/v0.12.0...v0.13.0) (2023-04-05)


### Bug Fixes

* choose unique default account from bridge when multiples are provided ([4be75cd](https://github.com/b-partners/bpartners-api/commit/4be75cd49880b2af7bb0f7e85e9870a83c4e88ae))
* refactor account holder repository and avoid NPE when swan not provided ([dc37b00](https://github.com/b-partners/bpartners-api/commit/dc37b0096cd0c8b7ad0a3dc24a27f0acf2015966))


### Features

* accountHolder has custom prospecting perimeter ([a3eb1ce](https://github.com/b-partners/bpartners-api/commit/a3eb1ceb21e32b92d4724bb9da3b3dabc5517aa6))



# [0.12.0](https://github.com/b-partners/bpartners-api/compare/v0.11.0...v0.12.0) (2023-04-04)


### Bug Fixes

* account bic is not override when transaction is from bridge ([0a018f9](https://github.com/b-partners/bpartners-api/commit/0a018f997f8cfe8d2040c015c9e4e7c23b428048))
* avoid NPE when retrieving transaction by ID from Swan ([1efb756](https://github.com/b-partners/bpartners-api/commit/1efb756a80f605cf43af84b8803de6470402a9e9))
* avoid null values when retrieving user by token ([9dd3720](https://github.com/b-partners/bpartners-api/commit/9dd3720eda0f0116fb45ecf849cf01dec2730aab))
* retrieve bridge account from persisted account ID ([1b957c4](https://github.com/b-partners/bpartners-api/commit/1b957c48193ed9b6eef332938fb88c39d69e48a5))
* retrieve transaction from bridge and database when swan not provided ([9733e1d](https://github.com/b-partners/bpartners-api/commit/9733e1deacb9c8d29b7f3693d9e3c6be5113c2ae))
* return bridge transaction absolute amount value ([0670778](https://github.com/b-partners/bpartners-api/commit/067077849a5e4d4d7b8e08a2ad7fe4837b5e6ebb))


### Features

* archive customers ([8f9d169](https://github.com/b-partners/bpartners-api/commit/8f9d169cc3bee418e3cf06f330b9777d12714525))
* update account holder global info ([0b68661](https://github.com/b-partners/bpartners-api/commit/0b686613febdcd82dbffa7a771dd9ebf57b97f7e))



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



