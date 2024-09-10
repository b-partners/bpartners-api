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



