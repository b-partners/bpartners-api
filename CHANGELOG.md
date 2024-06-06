# [0.50.0](https://github.com/b-partners/bpartners-api/compare/v0.49.0...v0.50.0) (2024-06-05)


### Features

* add additional properties to area picture annotation metadata ([e3971b8](https://github.com/b-partners/bpartners-api/commit/e3971b87eeb927662bc4817b0c6e66f641673c45))



# [0.49.0](https://github.com/b-partners/bpartners-api/compare/v0.48.0...v0.49.0) (2024-06-04)


### Features

* add geopositions, currentTile and topLeftTile details to AreaPicture ([3e900a2](https://github.com/b-partners/bpartners-api/commit/3e900a27479b7e00eca4b455bba5ed6b687d2bc6))



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



