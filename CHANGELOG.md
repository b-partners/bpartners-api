# [0.48.0](https://github.com/b-partners/bpartners-api/compare/v0.47.0...v0.48.0) (2024-05-30)


### Features

* permit area picture extension ([45ee400](https://github.com/b-partners/bpartners-api/commit/45ee400d194031632cdff4635cd99682b7d07881))



# [0.47.0](https://github.com/b-partners/bpartners-api/compare/v0.46.0...v0.47.0) (2024-05-23)


### Features

* use Zoom instead of ZoomLevel, Zoom groups its level and its value as a number ([df42f91](https://github.com/b-partners/bpartners-api/commit/df42f9147b2c0465871e2add8f12fb052bd9ed3a))



# [0.46.0](https://github.com/b-partners/bpartners-api/compare/v0.45.0...v0.46.0) (2024-05-23)


### Features

* link AreaPicture with AreaPictureImageLayer and refactor area picture image get ([154f301](https://github.com/b-partners/bpartners-api/commit/154f3017ad9e0c2fd8c2d0e90a130754f69348e8))



# [0.45.0](https://github.com/b-partners/bpartners-api/compare/v0.44.0...v0.45.0) (2024-05-16)


### Features

* crupdateAreaPictureDetails returns AreaPictureDetails ([c69eb48](https://github.com/b-partners/bpartners-api/commit/c69eb48b1a302143a5e14c08ab6b1a0ec421eabf))



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



