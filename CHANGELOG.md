# [0.20.0](https://github.com/b-partners/bpartners-api/compare/v0.19.0...v0.20.0) (2023-07-13)


### Bug Fixes

* add default label fintecture payment when its value is null or under 3 chars ([e4393cb](https://github.com/b-partners/bpartners-api/commit/e4393cb70f1194a4748a3f75a75a819bdd7107ab))
* add unique constraint on bridge transaction id ([8593514](https://github.com/b-partners/bpartners-api/commit/8593514fd7a154d4c0a852a3377db971084a35f4))
* allow duplicated customers and customers with same email ([0d28387](https://github.com/b-partners/bpartners-api/commit/0d28387514dd9cc388bcfb0cc9167302ad021fd1))
* allow some nullable values when importing/crupdating customers ([06bba00](https://github.com/b-partners/bpartners-api/commit/06bba00a61af1fa0fa36f9ca39118214e59b1b6a))
* allow updating tva number ([3b13800](https://github.com/b-partners/bpartners-api/commit/3b13800cecd35bcb2b058a047f0dba2e9669f025))
* avoid NPE when evaluating prospects from excel ([437a016](https://github.com/b-partners/bpartners-api/commit/437a016a13773595e1c316d1679410dadc63162f))
* handle more than 10 simultaneous customers crupdate ([4601156](https://github.com/b-partners/bpartners-api/commit/460115644f62da6062d4106f72bb5ba8d0a40588))
* map accounts and account holders from jpa before updating user token ([89304f2](https://github.com/b-partners/bpartners-api/commit/89304f2ac0c8d39ea5e3c2e4b248f27c61b8bc47))
* map logo and monthly subscription for user correctly ([cc9e11c](https://github.com/b-partners/bpartners-api/commit/cc9e11c72d4aaf6c9b9246e42722009781686bc1))
* remove sending date and payment date check when marking invoice as paid ([dd36b05](https://github.com/b-partners/bpartners-api/commit/dd36b05d9a93a247dc51603a1fd349614fd4f26e))
* replace characters not supported by fintecture when initiating payment ([7993d7a](https://github.com/b-partners/bpartners-api/commit/7993d7ae1627c96fc5186c953219a27e6ad772a0))
* retry payment initiation request once when error occurs ([a41de41](https://github.com/b-partners/bpartners-api/commit/a41de410dfcb1d942776d70200e114faff379b96))
* validate invoice reference before crupdating ([7b54416](https://github.com/b-partners/bpartners-api/commit/7b54416dc40d3c0d9e8d38c5ab977818b78b6325))


### Features

* evaluate prospect through excel file ([775e98c](https://github.com/b-partners/bpartners-api/commit/775e98c2acc4610a5f7061cbb5848a07dfb1b606))



# [0.19.0](https://github.com/b-partners/bpartners-api/compare/v0.18.0...v0.19.0) (2023-06-29)


### Bug Fixes

* add beneficiary validator before initiating fintecture payment ([a5e9c63](https://github.com/b-partners/bpartners-api/commit/a5e9c632dbc29d5eec2bf820a04087dfb16a4d34))
* add validator to company info update and update only not null values ([123eb93](https://github.com/b-partners/bpartners-api/commit/123eb93da76c3eb1f475011a18de540c91277e7b))
* avoid null values in active accounts list ([a7e84f5](https://github.com/b-partners/bpartners-api/commit/a7e84f5271cf9968dff9d3728ff78d5856b2b229))
* confirm invoice with default account and get S3 key from database when relaunching invoice ([e0520f5](https://github.com/b-partners/bpartners-api/commit/e0520f57aaa75056fd46cf31c0552d033fbf5716))
* map accounts and account holders from jpa before updating user token ([642d86e](https://github.com/b-partners/bpartners-api/commit/642d86ee4b5ea92ec412a11caaef0cd74fff9243))
* migrations naming and orders ([753e7f4](https://github.com/b-partners/bpartners-api/commit/753e7f4fcfba8ab1cf3ceb0519b8badaf734524e))
* persist default chosen account as preferred user account by default ([f276c3d](https://github.com/b-partners/bpartners-api/commit/f276c3dbd93b1cc345c3a9bcb03188b58290e7d5))
* remove existing accounts before saving new one when disconnecting bank ([51f545a](https://github.com/b-partners/bpartners-api/commit/51f545a538bc11a57cbc2f6ba299ab267677a747))
* remove migration for deleting user without accounts and customer with duplicated emails ([abe91e7](https://github.com/b-partners/bpartners-api/commit/abe91e746714cb0f5a9099455b4f3775073f13ac))
* return empty list when errors occur when getting payments from fintecture ([d4faebd](https://github.com/b-partners/bpartners-api/commit/d4faebd5ca6ae194c7616e4c88bd1fda94634b7f))
* some customers can not have same email address ([6aa0eec](https://github.com/b-partners/bpartners-api/commit/6aa0eec3cae484412c1e9cbd2631439a60a7e0e3))
* transaction summary scheduled for 1 hour interval not 1 minute ([25960a9](https://github.com/b-partners/bpartners-api/commit/25960a9cc8dd08befbfc03fcc4919ee6b0ac0d2b))
* transactions summary cash flow is correctly mapped ([99aea01](https://github.com/b-partners/bpartners-api/commit/99aea013f3a45b85fcbd9a41bc9fa171ae2653d6))
* use user ID when generating attachment when relaunching invoice ([9bfe77a](https://github.com/b-partners/bpartners-api/commit/9bfe77a9feaf63d0362f4915022151708355c5bf))


### Features

* implement whois endpoint ([fafc306](https://github.com/b-partners/bpartners-api/commit/fafc306861cf6312c69142ea467daafd298e61ea))
* send email to admin when successfully onboarded new user or crupdate customer ([ec41759](https://github.com/b-partners/bpartners-api/commit/ec417590d0175f5a190158f476edef0241dbcfcb))



# [0.18.0](https://github.com/b-partners/bpartners-api/compare/v0.15.0...v0.18.0) (2023-05-31)


### Bug Fixes

* add missing attributes validator when initiating payment ([bf018a5](https://github.com/b-partners/bpartners-api/commit/bf018a595e7b32808ee9b18fcd658e081da1f614))
* allow bank disconnection only when account is associated to a bank ([5473592](https://github.com/b-partners/bpartners-api/commit/547359296b699f19d359268734167c819c014071))
* associate new account to appropriate user ([2fb2160](https://github.com/b-partners/bpartners-api/commit/2fb2160268febd82d73f9e1926e6907564297d63))
* associate transactions summary to user ([e3c1a2d](https://github.com/b-partners/bpartners-api/commit/e3c1a2d706e79c270eb5e9f015d8b9260eedd776))
* avoid NPE when retrieving bridge account by ID from scheduled task ([72402df](https://github.com/b-partners/bpartners-api/commit/72402df942e107c76835638ef96cd939ab71ca55))
* disable ProspectService.prospect cron ([73038ad](https://github.com/b-partners/bpartners-api/commit/73038ad51cc9978241ffb1d35dabf80ef5f15e0a))
* get bank from database when mapping user accounts ([1a52348](https://github.com/b-partners/bpartners-api/commit/1a5234831543992db11d0cf543ae211199acfedc))
* import products ([#782](https://github.com/b-partners/bpartners-api/issues/782)) ([3749c1c](https://github.com/b-partners/bpartners-api/commit/3749c1c29acce22c2b94986b8f122065d180d6ea))
* minor and major AGAIN AND AGAIN ([776361b](https://github.com/b-partners/bpartners-api/commit/776361b5d61b8385aed1c8bc405af67af3eb8d12))
* persist bridge transactions values when finding transactions ([4400d83](https://github.com/b-partners/bpartners-api/commit/4400d832ecd018b17052e66dc2706313d296c8a4))
* refresh all accounts refresh transactions summary not only for active ([9498146](https://github.com/b-partners/bpartners-api/commit/9498146343251caab8e7f2a2839d33c091fa510c))
* return null when any token provided when retrieving bridge account by ID ([bdb1810](https://github.com/b-partners/bpartners-api/commit/bdb18109ae31cc31d653bbc637dc17960655f7f5))


### Features

* add script to create function for getting billing info  ([380239e](https://github.com/b-partners/bpartners-api/commit/380239ea158c40158224a7ad7f82fb6faad5ea73))
* archive invoices of an account ([#672](https://github.com/b-partners/bpartners-api/issues/672)) ([803a674](https://github.com/b-partners/bpartners-api/commit/803a6746373d0f3f1c62a2640d1331b5a07cf2ec))
* choose active account for an user ([95bd3d5](https://github.com/b-partners/bpartners-api/commit/95bd3d591674551eaa0950e9c7d6943178022382))
* get one specific customer or one specific product for an account  ([a8bed88](https://github.com/b-partners/bpartners-api/commit/a8bed8857df52d366a319e2851de6109139320c7))
* get one specific transaction for an account ([5134f46](https://github.com/b-partners/bpartners-api/commit/5134f465db02d97ac87ea647b2685e138ec3323e))
* handle bridge SCA synchronization ([979bad8](https://github.com/b-partners/bpartners-api/commit/979bad832f71ffad013f7db76c9d176a7f09c73a))
* handle multiple accounts for an user ([7353cfd](https://github.com/b-partners/bpartners-api/commit/7353cfdd6b7e13c4e54e64770dd41ee1f279d4e5))



# [0.15.0](https://github.com/b-partners/bpartners-api/compare/v0.14.1...v0.15.0) (2023-05-04)


### Bug Fixes

* reset account values when initiating bank connection ([4cc8c5c](https://github.com/b-partners/bpartners-api/commit/4cc8c5c18a7481ea200095d04fc00e8e08b0ebcc))
* set bank value in database as NULL when disconnecting bank from bridge ([ed43039](https://github.com/b-partners/bpartners-api/commit/ed4303962dde90956b105cafc2c4e52b0d6dbfe4))


### Features

* allow account validation ([#786](https://github.com/b-partners/bpartners-api/issues/786)) ([5d4a165](https://github.com/b-partners/bpartners-api/commit/5d4a165468d60eb63729b1bc75ecaf1e1f94e972))



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



