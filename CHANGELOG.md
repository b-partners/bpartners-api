# [0.44.0](https://github.com/b-partners/bpartners-api/compare/v0.43.0...v0.44.0) (2024-05-07)


### Bug Fixes

* annotation instance metadata are not mandatory ([b978c5e](https://github.com/b-partners/bpartners-api/commit/b978c5e5ecc68a00295f8b5915f249012aa02272))
* put empty string as firstName and lastName to avoid null values ([4e72a81](https://github.com/b-partners/bpartners-api/commit/4e72a8113ef6739893acc88d6d6604b7acf15711))


### Features

* add first name to prospect ([fd9942b](https://github.com/b-partners/bpartners-api/commit/fd9942b3ef34af15dd64c076fbc947016422705a))
* update customer firstName when prospect is created or updated ([029300b](https://github.com/b-partners/bpartners-api/commit/029300b0d482d5cdd24d109918820b5297368ece))



# [0.43.0](https://github.com/b-partners/bpartners-api/compare/v0.42.0...v0.43.0) (2024-04-30)


### Bug Fixes

* unable to assert point equality ([dd3878b](https://github.com/b-partners/bpartners-api/commit/dd3878bc938131dee640e73fa20ef78c93d32b9e))


### Features

* add colors, obstacle and comment to annotation instance metadata ([cc205bf](https://github.com/b-partners/bpartners-api/commit/cc205bf797c139eca133c1598b232f3f54c37961))



# [0.42.0](https://github.com/b-partners/bpartners-api/compare/v0.41.0...v0.42.0) (2024-04-24)


### Bug Fixes

* bad file naming in area_picture ([c83a4eb](https://github.com/b-partners/bpartners-api/commit/c83a4ebc6905576b0af5f4f7d4134430efd0400d))
* missing mapped data from prospect to customer ([037897f](https://github.com/b-partners/bpartners-api/commit/037897f50496f431ccf0658cf8cf12b30b45d317))
* NPE on customer.location when prospect.location is null ([615730f](https://github.com/b-partners/bpartners-api/commit/615730fc4b28efc9e60a2cf690912f778e277dbf))


### Features

* add labelType to AreaPictureAnnotationInstance ([d620268](https://github.com/b-partners/bpartners-api/commit/d620268717c6f9efd4bfa4565bab7b3b275e1855))
* annotate area pictures and read made annotations ([eb7e8e7](https://github.com/b-partners/bpartners-api/commit/eb7e8e7ea3f1c1d9ee70161ac83ec740b71e266e))
* crupdate prospect crupdates customer, however if customer exists, it updates the isConverted field only ([040faa9](https://github.com/b-partners/bpartners-api/commit/040faa9e7069c006eb33b50dd316fa51201fc0c6))
* default osm layer TOUS_FR containing all regions, but quality might be poor ([9d0d70f](https://github.com/b-partners/bpartners-api/commit/9d0d70fbcf1bd80ed647a661dc2aa3d7df841be3))
* filter customers by id prospect ([60dfa6c](https://github.com/b-partners/bpartners-api/commit/60dfa6c2b11b41ee2c5dd4e8579d01a9c326b7f8))
* map invoice with area picture ([74786f8](https://github.com/b-partners/bpartners-api/commit/74786f8280d2c568234bfaf4a0e505c073591470))
* set openstreetmap layer from given layer or from geoposition via src/main/resources/files/france-geojson/departements.geojson, only use tous_fr for now and log most used regions ([13bc3c9](https://github.com/b-partners/bpartners-api/commit/13bc3c9f65493fff2d48353f5ce02f31919bc256))



# [0.41.0](https://github.com/b-partners/bpartners-api/compare/v0.40.0...v0.41.0) (2024-04-05)


### Features

* add mandatory prospect_id to area_picture ([13e8d26](https://github.com/b-partners/bpartners-api/commit/13e8d2636593c705c66e3386fc8f732a7e0294cc))


### Reverts

* Revert "chore(to-revert): disable filename pattern matching" ([a4f07d4](https://github.com/b-partners/bpartners-api/commit/a4f07d41b780d8e62a13f849a7d56eddd76a5b81))



# [0.40.0](https://github.com/b-partners/bpartners-api/compare/v0.39.0...v0.40.0) (2024-04-04)


### Bug Fixes

* avoid NPE during old customer import evaluation ([258e977](https://github.com/b-partners/bpartners-api/commit/258e97777ddb7f37a557f3def280c05d4254cbd9))
* disconnect bank when bridge items is empty ([1da7948](https://github.com/b-partners/bpartners-api/commit/1da7948ed6a8020b9aea405ed5bf44f650237a48))
* implement invoice summary scheduled task ([38dad3e](https://github.com/b-partners/bpartners-api/commit/38dad3ef0238b183e4a1ffef0a5bc8a94f27e0ac))
* optimize invoices summary without count value ([4b78a90](https://github.com/b-partners/bpartners-api/commit/4b78a903d94c13d6eca0725ed3d6899c97ceb66d))
* set prospect contact nature to prospect and reformat project ([1707b0a](https://github.com/b-partners/bpartners-api/commit/1707b0ad63cc9f6b0299b3156359a21bcde97e05))
* set prospect rating default value to 1 ([bda7576](https://github.com/b-partners/bpartners-api/commit/bda7576a97eef39bedc35804a43669b02e0d641b))
* **test:** event is not ack if eventserviceInvoker fails ([1f765c1](https://github.com/b-partners/bpartners-api/commit/1f765c17ef497d9a6880cc068c74848bb2a67745))
* **test:** make broken tests pass ([44d25d7](https://github.com/b-partners/bpartners-api/commit/44d25d7ec2dd28b2257db3b5f1ef3082d3f66602))


### Features

* area picture download from s3 or from osm ([e7140b3](https://github.com/b-partners/bpartners-api/commit/e7140b36075bdaf1c66a586625983a1b3d903da2))
* multipart upload ([117b2bf](https://github.com/b-partners/bpartners-api/commit/117b2bf154eaeb0cc40e7785b1fcf0f467446598))
* read area pictures ([a393e3e](https://github.com/b-partners/bpartners-api/commit/a393e3e4c6b12049b31aab8703dd959053e57639))



# [0.39.0](https://github.com/b-partners/bpartners-api/compare/v0.38.2...v0.39.0) (2024-01-23)


### Bug Fixes

* add invoice supporting docs inside transaction other supporting docs ([8083194](https://github.com/b-partners/bpartners-api/commit/808319490208adc51bdebff75e197e6c2c0c805f))
* avoid deleting old transactions supporting documents when disconnecting bank account ([853e61b](https://github.com/b-partners/bpartners-api/commit/853e61b2deb614d8e7bcf9e236caa6150cdae6ef))
* avoid NPE for transaction supporting docs when disconnecting bank account ([120d158](https://github.com/b-partners/bpartners-api/commit/120d1589f77d32c1ebc445295d41b2fa9f0d670f))
* bank disconnection ([cbfa18e](https://github.com/b-partners/bpartners-api/commit/cbfa18e63ac09a497ebb25814ea31e34253d8d09))
* enable default account after bank disconnection ([a54c450](https://github.com/b-partners/bpartners-api/commit/a54c4502a4a7789d9d75eb1ad1c517051eabb320))
* enable known accounts after bank connection ([ba20867](https://github.com/b-partners/bpartners-api/commit/ba208676291de3eb2d1f56f82c871f3e42aeb4ae))
* show company name into invoice PDF when customer is professional ([0a4aed6](https://github.com/b-partners/bpartners-api/commit/0a4aed64ec63e191259a9a9b62ab1e167e52de8c))
* update invoice and its pdf after fintecture payment reception ([b5334c4](https://github.com/b-partners/bpartners-api/commit/b5334c45875a407bfdbf2cdf382c2b25191f9fbf))


### Features

* get invoices summary ([01018f9](https://github.com/b-partners/bpartners-api/commit/01018f9b763f57b5f0504c22c27434e7e263e618))



## [0.38.2](https://github.com/b-partners/bpartners-api/compare/v0.38.1...v0.38.2) (2024-01-09)


### Bug Fixes

* set ses source correctly ([2cd4a2f](https://github.com/b-partners/bpartners-api/commit/2cd4a2faeebcc495455ace9afd184813733136a9))



## [0.38.1](https://github.com/b-partners/bpartners-api/compare/v0.38.0...v0.38.1) (2024-01-08)


### Bug Fixes

* do not set database username in application.properties ([fb8000c](https://github.com/b-partners/bpartners-api/commit/fb8000c5b7c9b31069f3babecf879ac0f89ef318))
* rename account ID for transactions supporting documents API ([9964cca](https://github.com/b-partners/bpartners-api/commit/9964cca5745ea5c2ec9e77248a33c961ae5e21e4))
* set checksum algorithm only when uploading file ([e3b8760](https://github.com/b-partners/bpartners-api/commit/e3b87602f8fbabc7c2d7031f841dd20aa6051765))



# [0.38.0](https://github.com/b-partners/bpartners-api/compare/v0.37.2...v0.38.0) (2024-01-03)


### Bug Fixes

* add name attribute to customer instead of first or last name ([262447f](https://github.com/b-partners/bpartners-api/commit/262447f746daf445f5690dee0529b9f2663edb79))
* avoid NPE when checking customers locations update ([fdb05f5](https://github.com/b-partners/bpartners-api/commit/fdb05f577822f82f416089c9fa57f4ccb16b7f54))
* do not check sogefi prospector when business activity is null ([25e13b8](https://github.com/b-partners/bpartners-api/commit/25e13b88039cf73a8b30c7ec40f572a3175209e8))
* do not rethrow error when terminating job in prospectEvaluationJob ([d38c99c](https://github.com/b-partners/bpartners-api/commit/d38c99c4a17bc2228431be7f4362966411816389))
* drop postgis extension cascade ([2dadc67](https://github.com/b-partners/bpartners-api/commit/2dadc67cd6e3450af41e6605d4212c72af44bc6e))
* only give INDIVIDUAL type for customer when provided type is null ([73d89ac](https://github.com/b-partners/bpartners-api/commit/73d89ac0323910f0308d72f26fc71c0b81192c19))
* order calendar list by OWNER permission ([eaa5f6f](https://github.com/b-partners/bpartners-api/commit/eaa5f6f104d9525027ebe23ecc24026ebcc7f911))
* refresh calendars when exchanging calendar token ([16762b4](https://github.com/b-partners/bpartners-api/commit/16762b419782aac55945f4c485bdd04bb1459972))
* refresh invoice after payment webhook update ([aba6815](https://github.com/b-partners/bpartners-api/commit/aba681517024e450beb6f0a08e7a0d12d0af242f))
* rename customer fullName method ([14fa14f](https://github.com/b-partners/bpartners-api/commit/14fa14f9a8f9b3de34254fd715eb859f2fb70fed))
* retrieve customer name for POST endpoint creation ([1be6a29](https://github.com/b-partners/bpartners-api/commit/1be6a29e4223f2bc52f0960ed131ef881d184655))
* send email when saving prospects ([a5fe147](https://github.com/b-partners/bpartners-api/commit/a5fe14784c2a4ee166e07ca9843d077ad87a9ae5))
* update customer position if latest value is null ([46ad8f5](https://github.com/b-partners/bpartners-api/commit/46ad8f5e7cba6ff73078f53684f933b87d904727))
* validate email body before sending email from scratch ([3cdd601](https://github.com/b-partners/bpartners-api/commit/3cdd601294069e69678c1c3246dc8c6d985ebd5f))
* verify calendar access token before running job service ([768c359](https://github.com/b-partners/bpartners-api/commit/768c35997ec989a27c8da299f0eff6ae7585e52b))


### Features

* add  and get external supporting documents to transaction ([9994bdc](https://github.com/b-partners/bpartners-api/commit/9994bdcf07c8a06f9b223d1c2f4c7be91f7fa316))
* add customer type to customers ([e035bb2](https://github.com/b-partners/bpartners-api/commit/e035bb270544c1f4db155c1563d9b206293b71e6))



## [0.37.2](https://github.com/b-partners/bpartners-api/compare/v0.37.1...v0.37.2) (2023-12-07)


### Bug Fixes

* define parameters inside api for transactionExportLink operations ([e838770](https://github.com/b-partners/bpartners-api/commit/e8387706e1197ae8f98a8d074dc6d3b07073b64e))



