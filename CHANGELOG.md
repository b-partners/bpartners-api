## [0.37.2](https://github.com/b-partners/bpartners-api/compare/v0.37.1...v0.37.2) (2023-12-07)


### Bug Fixes

* define parameters inside api for transactionExportLink operations ([e838770](https://github.com/b-partners/bpartners-api/commit/e8387706e1197ae8f98a8d074dc6d3b07073b64e))



## [0.37.1](https://github.com/b-partners/bpartners-api/compare/v0.37.0...v0.37.1) (2023-12-07)


### Bug Fixes

* add update datetime and sending datetime attributes to email ([752cc78](https://github.com/b-partners/bpartners-api/commit/752cc7874d6e5319250cbca38bfce6e01aa9f0b9))
* allow authenticated users to list their own emails ([e793e91](https://github.com/b-partners/bpartners-api/commit/e793e9181ad491e95e926a95a51db9096a92e5a0))
* compare business activity for sogefi prospecting when values not null only ([4ec3c13](https://github.com/b-partners/bpartners-api/commit/4ec3c1327dfb71b37cb994a6e5e8ec353602848b))
* do not update coordinates if customer address did not change ([81e095a](https://github.com/b-partners/bpartners-api/commit/81e095a09fb077c31c432498f0298a8eda921297))
* handle customerCrupdated event synchronously ([2b25448](https://github.com/b-partners/bpartners-api/commit/2b25448705dc17a802dfd4389e6628e2d7e4e36c))
* send email for every update on prospect ([03919ae](https://github.com/b-partners/bpartners-api/commit/03919ae392a8946834a612e2a2782bffc4d28e7a))
* set fintecture private key env dependant ([1a81f6a](https://github.com/b-partners/bpartners-api/commit/1a81f6a64cc9f9581baeadbeaa5c2299622e5716))



# [0.37.0](https://github.com/b-partners/bpartners-api/compare/v0.36.1...v0.37.0) (2023-12-05)


### Bug Fixes

* add admin email as invisible recipient when notifying prospects evluations result by email ([41de5e1](https://github.com/b-partners/bpartners-api/commit/41de5e18cd44792aa62009e7f3934c61eca3ae5c))
* add annulation message inside object ([f446429](https://github.com/b-partners/bpartners-api/commit/f446429aebfd525a0f3f215dee339a0b55f52455))
* add bank connection status UNDERGOING_REFRESHMENT ([388b3c7](https://github.com/b-partners/bpartners-api/commit/388b3c784629d050fb7124abb5c4f674b9f0f542))
* add default payment status update datetime when crupdating invoice ([5dc534d](https://github.com/b-partners/bpartners-api/commit/5dc534d09f1b1bbe0cef745b332f12128381970a))
* add prospect.latest_old_holder inside view_prospect_actual_status and its usage ([00767ea](https://github.com/b-partners/bpartners-api/commit/00767ea1efa0e95424034f9670e5e44d647159b0))
* add prospect.latest_old_holder inside view_prospect_actual_status and its usage ([f077e34](https://github.com/b-partners/bpartners-api/commit/f077e3492fd384ff94de2c463e519e9c42773ab9))
* add script to delete duplicated transaction summary ([f0de6e1](https://github.com/b-partners/bpartners-api/commit/f0de6e1cc1d8c07544295cbbbf217c5fb7ed03b3))
* add UNDERGOING_REFRESHMENT account and bank status ([b960b80](https://github.com/b-partners/bpartners-api/commit/b960b80748617cb0d9b93c446ae5f1bb531d102f))
* add UNKNOWN type of calendar permission ([7b865fa](https://github.com/b-partners/bpartners-api/commit/7b865fa9260b29e6dcbf8c5fae8967fa1e63dfae))
* after update attach prospect to account holder ([1d86105](https://github.com/b-partners/bpartners-api/commit/1d861050e710020940853d9d809d359d1018d75d))
* associate prospects directly to account holder ([0d8485e](https://github.com/b-partners/bpartners-api/commit/0d8485e8f56477779ecf0cd0a6f2e0ebcd1b0c65))
* avoid NPE when relaunching prospects ([04a5ec3](https://github.com/b-partners/bpartners-api/commit/04a5ec39e78ba2d0faf76dd1d65017d4b89f7bea))
* avoid NPE when sending email ([afa6440](https://github.com/b-partners/bpartners-api/commit/afa64405d336e5d59a7854ffad5fa21f3e13a15f))
* avoid NumberFormatException when converting postal code ([1055e36](https://github.com/b-partners/bpartners-api/commit/1055e36919c7beb91da11a88c01ea8a56554be5b))
* compute enable transactions only for transaction summary ([d554b04](https://github.com/b-partners/bpartners-api/commit/d554b04a6720870b5af08cd9dac1ba01afb24501))
* disable invoice refresh after each instace deployment ([82bdaa2](https://github.com/b-partners/bpartners-api/commit/82bdaa277ef48c09595415924d425b5bf6f44b79))
* disable mobile notification after payment received ([50b5c1d](https://github.com/b-partners/bpartners-api/commit/50b5c1d6667aa5360e5f1729bf44ff5566cd3f49))
* disable prospects relaunch when launching apps ([09333bc](https://github.com/b-partners/bpartners-api/commit/09333bc3870318a52597278706afff0a0f102384))
* do not show payment datetime when payment is not still paid ([76105a7](https://github.com/b-partners/bpartners-api/commit/76105a7fb6be0705824029327157db6109018b25))
* do not use authProvider.getUser inside S3 service and invoice service ([19bd40f](https://github.com/b-partners/bpartners-api/commit/19bd40f5b44aefab5c3a545dc53f7b415dfc7c56))
* handle null rating prospect ([bbc4f49](https://github.com/b-partners/bpartners-api/commit/bbc4f49cdff2e2ea702153b75f953a121238cdfe))
* handle oauth2 success url parameters through state ([d7f1132](https://github.com/b-partners/bpartners-api/commit/d7f11323b911c751a52113ad222e69327b03f33f))
* ignore holderId inside spreadsheet when evaluating prospects ([36902e2](https://github.com/b-partners/bpartners-api/commit/36902e22cdd17e528011c07b9a47c733812c8c01))
* let authorized user to relaunch invoice and hide duplicated account holder prefix ([d98007f](https://github.com/b-partners/bpartners-api/commit/d98007fabf49661791059fd49c74a8e8d858bb72))
* log exception when error occured in mobile notification ([7248579](https://github.com/b-partners/bpartners-api/commit/724857957bb17c3453fc3991369312e347d3d45e))
* read prospect dynamically from artisan sheet name ([bc96022](https://github.com/b-partners/bpartners-api/commit/bc96022224ef64fd7a4fc943ba35cffe6bf8673e))
* relaunch invoice synchronously ([7fe8e83](https://github.com/b-partners/bpartners-api/commit/7fe8e8381175283a9ffdab75d2cebd967a23fdd7))
* replace device token and endpoint arn after updates and delete old endpoint arn ([8d41609](https://github.com/b-partners/bpartners-api/commit/8d41609fa7f5aef6e0f5a263d97f44b036cc8d41))
* return prospects without business activities fitlers ([d4f6685](https://github.com/b-partners/bpartners-api/commit/d4f66853af89f98abb926b70b0a2da576219901f))
* save payment status updated datetime when saving invoice ([ab8f6e8](https://github.com/b-partners/bpartners-api/commit/ab8f6e8fa771e8949abcbdc39c377f65cca6d49b))
* send email when prospect is given up ([27124d7](https://github.com/b-partners/bpartners-api/commit/27124d70d2ae91850c726ffda65c5982befd378b))
* send email when prospect is given up even from TO_CONTACT to TO_CONTACT ([b3617a5](https://github.com/b-partners/bpartners-api/commit/b3617a513cc669f863472bacdb9ad418881aa1d9))
* send emails synchronously during onboarding and generate manually account ID ([2217ca5](https://github.com/b-partners/bpartners-api/commit/2217ca562310d8b72714b33acd65312ba2455f36))
* send prospect updated mail synchronous ([ff80233](https://github.com/b-partners/bpartners-api/commit/ff802339e14cacdfd1b813a4c5b32647299236ba))
* use google service-account credentials when using sheets API ([74eb092](https://github.com/b-partners/bpartners-api/commit/74eb0921ea4057efcfd03d58f7a9b03e2831c4b5))
* use invoice owner account - holder - users informations when relaunching ([8db166c](https://github.com/b-partners/bpartners-api/commit/8db166c720e61a0a7d8869710cc5574d58233265))


### Features

* generate transactions export link ([4c7a6f4](https://github.com/b-partners/bpartners-api/commit/4c7a6f4e5e54e2dca7849c7ca84191c1a29e9d0c))
* register device for a specific user ([2358ac8](https://github.com/b-partners/bpartners-api/commit/2358ac8794e99a23984f02c297584f481d0115b0))
* send and read emails for an user ([f5acbb9](https://github.com/b-partners/bpartners-api/commit/f5acbb94e33491c6e5c3f0bfbc6476011da76268))
* send notification to account holders devices when prospects not relaunched ([99a4c3d](https://github.com/b-partners/bpartners-api/commit/99a4c3dcac97704394622e61e42275992b19ddf1))
* send notification to device when successfully evaluating prospects ([281e6c5](https://github.com/b-partners/bpartners-api/commit/281e6c5bc1bec47d5f71066a890be811c43a7f7d))


### Reverts

* Revert "fix: add id_user criteria when comparing account with same iban or bank account infos" ([21df7e2](https://github.com/b-partners/bpartners-api/commit/21df7e2a0b138ce004fddd4b23039caefd074550))
* Revert "infra: 2 big prod instances" ([d2bc064](https://github.com/b-partners/bpartners-api/commit/d2bc064f844cfc188c5078ac574851ef6bf56a8d))



## [0.36.1](https://github.com/b-partners/bpartners-api/compare/v0.36.0...v0.36.1) (2023-11-08)


### Bug Fixes

* add contact nature inside prospect view ([707349c](https://github.com/b-partners/bpartners-api/commit/707349c56fd02943639d486aaf23fdab6e31e1cc))
* add payment datetime under invoice stamp ([da87dd2](https://github.com/b-partners/bpartners-api/commit/da87dd2d066d6dd31f30ee11dcc3c6cb99805097))
* attach saved prospect to account holder owner ([543a5e5](https://github.com/b-partners/bpartners-api/commit/543a5e56b90e3b337318b47d1de3041a0265fae5))
* handle -1 value for port not set when extracting url ([2730d62](https://github.com/b-partners/bpartners-api/commit/2730d62bd24e8aea03d3001b05c1f4563bf63f4f))
* handle query parameters when initiating calendar consent ([2d563fc](https://github.com/b-partners/bpartners-api/commit/2d563fca7d0b02b928f856707dff26bb10feae2c))
* set payment created datetime as default payment status updated datetime ([1594533](https://github.com/b-partners/bpartners-api/commit/159453301718e7269f9dc08a1cc143095f39aced))
* set payment value as cents in notification ([f78eb2f](https://github.com/b-partners/bpartners-api/commit/f78eb2f02d25f65b41c1ac858a9acf2f6ef7d824))



# [0.36.0](https://github.com/b-partners/bpartners-api/compare/v0.35.1...v0.36.0) (2023-11-03)


### Bug Fixes

* add contact nature to prospect ([53d9ab2](https://github.com/b-partners/bpartners-api/commit/53d9ab2225f21059637630f0fd6c3eaa04ec23c2))
* add encoded sns arn inside users attributes ([d90113b](https://github.com/b-partners/bpartners-api/commit/d90113b5c53cd17197de43c91fae4450628d0fd1))
* avoid NPE when crupdating prospects ([3a95d13](https://github.com/b-partners/bpartners-api/commit/3a95d130e2017e464494712963d2a7bdc590e4f6))
* crupdate prospects ([f12e445](https://github.com/b-partners/bpartners-api/commit/f12e44580956da5e81f7582956c2365b2982ad29))
* filter invoice keywords by customer infos ([6d2e061](https://github.com/b-partners/bpartners-api/commit/6d2e0613f859386657582f21142a18024968059c))
* order customers by updated datetime desc ([1279aef](https://github.com/b-partners/bpartners-api/commit/1279aefab718c07bf5c4e23e756651dfae3f3743))
* relaunch TO_CONTACT prospects to 2pm and only with rating > 0 ([cf8b1e0](https://github.com/b-partners/bpartners-api/commit/cf8b1e0f737b6517ae36a9d6e68736a2dc59e536))
* remove spreadsheet properties when prospecting through calendar ([a632059](https://github.com/b-partners/bpartners-api/commit/a6320598437d3f0b0bf0f8d5cabb9fea602b877f))
* set payment amount value inside email status changed to cents ([ad5348e](https://github.com/b-partners/bpartners-api/commit/ad5348eb8d75f64027dbfb0ace6166318d7acc1d))


### Features

* relaunch account holders every friday 02:00 PM for prospect with status TO_CONTACT ([b351a8d](https://github.com/b-partners/bpartners-api/commit/b351a8dbe8cc3b53b1d192176c5545e57692fccb))


### Reverts

* Revert "chore: set account holder prospects relaunch to 4pm" ([473dbac](https://github.com/b-partners/bpartners-api/commit/473dbac3265de66c2ab73bbcce94e3d4d3a00085))
* Revert "chore(to-revert): refresh unpaid invoices for artisan alphanuisible" ([2142ad5](https://github.com/b-partners/bpartners-api/commit/2142ad59106c0305ef1a093b1fc969ed0cfeb312))



## [0.35.1](https://github.com/b-partners/bpartners-api/compare/v0.35.0...v0.35.1) (2023-10-24)


### Bug Fixes

* add additional info to prospect sent after status change ([152b46c](https://github.com/b-partners/bpartners-api/commit/152b46c0d889d8c71be20585b2867546ae2b54c4))
* complete signature verification with payment_status_changes webhooks ([2672d53](https://github.com/b-partners/bpartners-api/commit/2672d5307fbdfc76c38960c9dc4e0f7e3e5bbf75))
* convert address to coordinates when reading prospects through sheets ([0c34efa](https://github.com/b-partners/bpartners-api/commit/0c34efad58cfae847530b2ed35d8dafa56c72bea))
* enrich invoice reference check during crupdate ([d650788](https://github.com/b-partners/bpartners-api/commit/d6507889bd58752dcd7abaa8cf6ff44e75d6b589))
* let invoice crupdate and listing without authenticated user ([550dacf](https://github.com/b-partners/bpartners-api/commit/550dacfd49b70e831e96c54dec8ab53389dbdb5c))



# [0.35.0](https://github.com/b-partners/bpartners-api/compare/v0.34.1...v0.35.0) (2023-10-17)


### Bug Fixes

* remove duplicated email subject when relaunching invoice ([d77ccb9](https://github.com/b-partners/bpartners-api/commit/d77ccb95f12b4bd30277048ecb4a3f064aafc2be))
* set google calendar provider when listing events by default ([ab1620a](https://github.com/b-partners/bpartners-api/commit/ab1620addce3cbef08d9102b6d0f30c153b285a5))
* throw bad request exception instead of empty array when listing calendar events with expired token ([ba59a3c](https://github.com/b-partners/bpartners-api/commit/ba59a3c3a4bd8acac418dfa29e59ccd2078f9e0d))


### Features

* add website to account holder ([0c9e563](https://github.com/b-partners/bpartners-api/commit/0c9e5635d2133469538df17aaca9bde82a0ceaa2))
* persist and return prospect status changes ([394e0b3](https://github.com/b-partners/bpartners-api/commit/394e0b3f2554cfb8a05d4e09419a93a99426d22c))



## [0.34.1](https://github.com/b-partners/bpartners-api/compare/v0.34.0...v0.34.1) (2023-10-16)


### Bug Fixes

* rename account identifier param in products export ([702e311](https://github.com/b-partners/bpartners-api/commit/702e311e4c328f89e1156fa6212cf1faec338381))
* unable to save events with expired google calendar token ([2aae567](https://github.com/b-partners/bpartners-api/commit/2aae567ceb54ea5268f6ed0f50248111bc68ddcf))
* update TO_CONTACT prospect informations correctly ([271f7da](https://github.com/b-partners/bpartners-api/commit/271f7da6fe193b38ca4ae1e7d69834191a71c021))



# [0.34.0](https://github.com/b-partners/bpartners-api/compare/v0.33.0...v0.34.0) (2023-10-12)


### Bug Fixes

* compare prospect status to update with existing prospect status ([a425c6a](https://github.com/b-partners/bpartners-api/commit/a425c6a646ed29a07c9b17e07eb4b14945115c6c))
* do not scan geoposition through BAN API when reading spreadsheet ([06a2c66](https://github.com/b-partners/bpartners-api/commit/06a2c66ee36e8c6e7fce4deff2aeb9e1ccf0250c))
* set customers location update scheduled task to 30 minutes intervals ([f5e8841](https://github.com/b-partners/bpartners-api/commit/f5e8841ddaacb7e4d3eda03b14cc536982158bc8))
* skip address less than 3 chars and more than 200 chars when updating customer position ([51f96cc](https://github.com/b-partners/bpartners-api/commit/51f96ccadca23b8729e784f2d8ee5b566ebdf31f))
* update customer geoposition after crupdate ([a0443ee](https://github.com/b-partners/bpartners-api/commit/a0443ee948d71d30b9196314483af52760dfe7c7))
* use google calendar provider when converting event to prospect ([e8619b6](https://github.com/b-partners/bpartners-api/commit/e8619b65184535ed081e8723b7cef82db94f108b))


### Features

* add default comment to prospect ([8fcc202](https://github.com/b-partners/bpartners-api/commit/8fcc2028bcaea381fedeb45cc7248bc57bcb7df2))
* add manager name to prospect ([87d6ac8](https://github.com/b-partners/bpartners-api/commit/87d6ac8f6194fff284dfb8d9c1fe6c4ab31c9f15))
* set invoice relaunch email body from scratch ([eb54aa6](https://github.com/b-partners/bpartners-api/commit/eb54aa69d78b1026e215789e9d95f737938a2d62))


### Reverts

* Revert "chore(to-reset): re deploy old api at api-prod" ([13e86cb](https://github.com/b-partners/bpartners-api/commit/13e86cb989a60f07826f0e8e8744cf605a97698f))
* Revert "chore(to-reset): use 20 as listenerRulePriority to old-api" ([f431c21](https://github.com/b-partners/bpartners-api/commit/f431c21fd4b2daabe3da4e3f4443543666680937))



# [0.33.0](https://github.com/b-partners/bpartners-api/compare/v0.32.2...v0.33.0) (2023-10-05)


### Bug Fixes

* avoid NPE when getting calendar events through google calendar ([14b3f35](https://github.com/b-partners/bpartners-api/commit/14b3f35f210f6e9f85562626a1310314a4151d4e))
* do not filter old customers by distance when evaluting prospects ([0e58062](https://github.com/b-partners/bpartners-api/commit/0e5806245215381077981b4c61d7ca695d799b23))
* return metadata when launching new prospect evaluation job ([b0d980a](https://github.com/b-partners/bpartners-api/commit/b0d980a87984048be10510a62fe39b70863c2762))


### Features

* import prospects through google sheet ([a81e1b7](https://github.com/b-partners/bpartners-api/commit/a81e1b70c6f44dff906915c20451e8b375fd42e1))
* send email to admin after each prospect update status ([607a61c](https://github.com/b-partners/bpartners-api/commit/607a61cf132fa5d73aeef0e9b44c2b947e654f13))


### Reverts

* Revert "infra: update listener rule target to xx-api.{env}.xxx"  ([fb59abc](https://github.com/b-partners/bpartners-api/commit/fb59abcd67dac31daf815e7648654ddd365722ac))



