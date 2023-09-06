# [0.27.0](https://github.com/b-partners/bpartners-api/compare/v0.26.1...v0.27.0) (2023-09-06)


### Features

* update prospect status with new attributes ([e34ce11](https://github.com/b-partners/bpartners-api/commit/e34ce11ee49122cf81f3cfbd2c2d6d5735d9b0e2))



## [0.26.1](https://github.com/b-partners/bpartners-api/compare/v0.26.0...v0.26.1) (2023-08-30)


### Bug Fixes

* **api:** archiveStatus query param schema in getInvoices operation ([a5dd1f9](https://github.com/b-partners/bpartners-api/commit/a5dd1f9ef3f60223c5754f504f421d7f3322432c))



# [0.26.0](https://github.com/b-partners/bpartners-api/compare/v0.24.0...v0.26.0) (2023-08-30)


### Bug Fixes

* center payment regulation qr code and move paid-stamp position ([b5f9d3b](https://github.com/b-partners/bpartners-api/commit/b5f9d3b18649c6411d9733d29c4877a0d2b4f2e9))
* CONFIRMED invoice can be created from sractch and duplicated invoice does not loose its products ([1573a3e](https://github.com/b-partners/bpartners-api/commit/1573a3e6204dd3d48a801298f8905ff2db870634))
* connexion timeout when starting app ([8fede17](https://github.com/b-partners/bpartners-api/commit/8fede175cb327ff52baa6a9456a5cfe7f8ba86ef))
* fix broken tests  ([4896921](https://github.com/b-partners/bpartners-api/commit/4896921ba4e151e90493f1b1c296752a8134d5b1))
* generate new fileId when duplicating invoice ([347a8cd](https://github.com/b-partners/bpartners-api/commit/347a8cdf37c30c5d4cb9f2fb4a3b33cc1f87d4c6))
* map correctly paymentMethod and paymentStatus for each payment regulation ([15ebc9d](https://github.com/b-partners/bpartners-api/commit/15ebc9dec99296da1ba32d59ba3d8d48cb23e3d4))
* put paid invoice regulation stamp on qr code zone ([cd1b145](https://github.com/b-partners/bpartners-api/commit/cd1b1450ea2f134f847c50cffbf887ca80deec78))
* reformat correctly credential exception in calendar API ([7fc0b8f](https://github.com/b-partners/bpartners-api/commit/7fc0b8f4d39f44171614bfda223a1033abea19af))
* rename correctly all calendar endpoints ([b82c596](https://github.com/b-partners/bpartners-api/commit/b82c59639036ef361fd2a6a94afd541a6d634bf3))
* return payment status through paymentRegStatus ([28d940c](https://github.com/b-partners/bpartners-api/commit/28d940cee1a285abd485bca7524cf36364a8d19a))
* update PDF after marking invoice payment regulation as paid ([29ec282](https://github.com/b-partners/bpartners-api/commit/29ec2827564500d197afa4cd976d3229e210cbad))


### Features

* duplicate existing invoice as draft ([d00f887](https://github.com/b-partners/bpartners-api/commit/d00f887865600ba5791866ae2e4668eea388d99b))
* handle EVAL_PROSPECT authorization for specific user ([fbf7600](https://github.com/b-partners/bpartners-api/commit/fbf7600df213dc836400b3af3b8a3d1c24fb01da))
* list all calendars without persisting ([dee0853](https://github.com/b-partners/bpartners-api/commit/dee0853f06dc5c2370e987a6fea17fda8fb3285c))
* read invoice by multiple statuses ([073a643](https://github.com/b-partners/bpartners-api/commit/073a64347fc9d10b71cf9fcb4000b08967466e68))
* read invoice filtered by archive status ([67ef348](https://github.com/b-partners/bpartners-api/commit/67ef3482b28de08d3de9354f4a3c23d3d2c96915))
* update payment regulation status ([ad32664](https://github.com/b-partners/bpartners-api/commit/ad32664f97a36bd82685a999194457d9fd95e52c))



# [0.24.0](https://github.com/b-partners/bpartners-api/compare/v0.23.0...v0.24.0) (2023-08-16)


### Bug Fixes

* avoid NPE when BanApi does not return result ([2aa2bf7](https://github.com/b-partners/bpartners-api/commit/2aa2bf764fb76fdd604350ca53c777615d5fd49e))
* handle bad address during new prospect evaluation ([b527513](https://github.com/b-partners/bpartners-api/commit/b5275133702284cb4f663ee848ce63f5e9227abb))
* map bridge transaction amount from minor ([ce98bda](https://github.com/b-partners/bpartners-api/commit/ce98bda5129d218320dd111326c2cd10c38173bc))
* return customers infos during old customers evaluation ([abfea53](https://github.com/b-partners/bpartners-api/commit/abfea539f2da1bffc106ee7942cf7028c40f5823))
* return null when cell type is error type when evaluating prospect ([c6d9071](https://github.com/b-partners/bpartners-api/commit/c6d907124ce62e19b4416664be57a52b771d48a3))
* throw NotFoundException when banApi result is null ([1c7202c](https://github.com/b-partners/bpartners-api/commit/1c7202c058c49e720227b52ea5fe2c2af64ed9e9))
* update customer location from full adress ([b7137de](https://github.com/b-partners/bpartners-api/commit/b7137deb79e4c4cf772c3c6b6e8aa52077b2a807))
* update customers location every 24 hours instead of 10 minutes ([ba6a682](https://github.com/b-partners/bpartners-api/commit/ba6a6820add735d252e9c68e40d891b4ece4daea))


### Features

* configure calendar Oauth2 and test by listing events ([edcfb65](https://github.com/b-partners/bpartners-api/commit/edcfb652873fd19f426633e9b71495760ae656f6))
* create new calendar events without persisting ([a3b4894](https://github.com/b-partners/bpartners-api/commit/a3b48948871225112a13b9bfa115ac3d8c1516ed))
* evaluations can be configured by NEW_PROSPECT, OLD_CUSTOMERS or ALL of these two ([9ab2bd0](https://github.com/b-partners/bpartners-api/commit/9ab2bd0dff29e7f228dfa37c00da33dbaffd6bdf))
* improve calendar events implementation and add filters ([eb44436](https://github.com/b-partners/bpartners-api/commit/eb4443638997a467ff49e78021cfdd4a97cefb5c))



# [0.23.0](https://github.com/b-partners/bpartners-api/compare/v0.21.0...v0.23.0) (2023-08-03)


### Bug Fixes

* add validation in excel file for prospect evaluations ([7843a45](https://github.com/b-partners/bpartners-api/commit/7843a45f45565dcf9c55459449fa177ae5822296))
* avoid NPE when mapping prospect to rest ([6ce1be2](https://github.com/b-partners/bpartners-api/commit/6ce1be2ab790d8e1c97891d6cf7087b07f6fc2a6))
* return comment when mapping payment regulation in invoice ([44adc40](https://github.com/b-partners/bpartners-api/commit/44adc40e10aa02dd1e8429d7a9a4dc628947b8ae))
* return null when invoking invoice stamp not paid ([cd1a851](https://github.com/b-partners/bpartners-api/commit/cd1a8514c51637d1689079438c25ef205f59e3ba))
* save major account balance value in database when getting from bridge ([9112999](https://github.com/b-partners/bpartners-api/commit/9112999d4a518083dbc35e5119bfa8835f8c06fc))
* set default prospect evaluation infos to order correctly prospect by rating desc ([5cdecf5](https://github.com/b-partners/bpartners-api/commit/5cdecf58a57ed5ba00e12fd967fd68ed474da4a3))
* set prospect eval info mailSent attribute as string not boolean ([2ffb7ed](https://github.com/b-partners/bpartners-api/commit/2ffb7ed6dd897637ace8178bcfe11f1d6aa0dff0))
* show all prospects when account holder business activity is anti-harm ([a176116](https://github.com/b-partners/bpartners-api/commit/a17611692575d6abb9cc88eed951fbcf8df7c294))
* show delay payment message in generated pdf when delay is 30 days ([6a142ab](https://github.com/b-partners/bpartners-api/commit/6a142ab5879009649983eb8dc5002e4964d9a55b))
* temporarily return all business activities ([8692576](https://github.com/b-partners/bpartners-api/commit/8692576ff65d91607ec27c37869c8651694abd71))


### Features

* add PAID stamp without payment method for invoice ([fcbf9a7](https://github.com/b-partners/bpartners-api/commit/fcbf9a762b65e391a037aa4cdde0b7f33a14325d))
* add payment method to invoice ([4a57223](https://github.com/b-partners/bpartners-api/commit/4a5722378d6968b2a23b5d54ca48e22f8c6c063b))
* add stamp when marking invoice as paid according to payment method ([72e957c](https://github.com/b-partners/bpartners-api/commit/72e957c19d9fb4b91749207d819232a89db85687))



# [0.21.0](https://github.com/b-partners/bpartners-api/compare/v0.20.0...v0.21.0) (2023-07-18)


### Bug Fixes

* do not compare prospect when rating is null ([f521d63](https://github.com/b-partners/bpartners-api/commit/f521d6392b0c6d1011afc5cd0f8f8ac722f3cbc0))
* do not compare prospect when rating is null ([87a0c26](https://github.com/b-partners/bpartners-api/commit/87a0c26d996b27544062511da34e2d0c050d4c7e))
* order prospects by rating desc ([2acbf4f](https://github.com/b-partners/bpartners-api/commit/2acbf4fa2b0e76a52785d67d3bcedfac0cdb0874))


### Features

* add rating informations to prospect ([b06a8f4](https://github.com/b-partners/bpartners-api/commit/b06a8f4c82cd4fd0bc10d9bd3781dc032b9a21c4))



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



