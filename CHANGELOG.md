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



## [0.32.2](https://github.com/b-partners/bpartners-api/compare/v0.32.1...v0.32.2) (2023-09-26)


### Bug Fixes

* use PUT instead of POST when running evaluation jobs ([7a169fe](https://github.com/b-partners/bpartners-api/commit/7a169fec69b5b8a3c79887052b022e19b7b57555))



## [0.32.1](https://github.com/b-partners/bpartners-api/compare/v0.32.0...v0.32.1) (2023-09-26)


### Bug Fixes

* dispatch calendar event list by provider ([d6fb338](https://github.com/b-partners/bpartners-api/commit/d6fb33879e0d74f956326f546145f469f9a0f21e))



# [0.32.0](https://github.com/b-partners/bpartners-api/compare/v0.31.0...v0.32.0) (2023-09-25)


### Bug Fixes

* add prospect or old customers notation inside email response when evaluating prospects ([b1d1013](https://github.com/b-partners/bpartners-api/commit/b1d101382d75e2dcd86b200f95e4cfc7efcc0c9e))
* **api:** rename ProspectEvaluationJobResult to ProspectEvaluationJobDetails and its operations ([ac54fa2](https://github.com/b-partners/bpartners-api/commit/ac54fa2a9b779e46d689f78b44366b34615e67d6))
* avoid duplicated prospects or old customers when evaluating prospects ([06039b3](https://github.com/b-partners/bpartners-api/commit/06039b306f5005473506d4b61849221ce10df97d))
* dispatch sending email by retrieved calendar event ([e7708dc](https://github.com/b-partners/bpartners-api/commit/e7708dc0496b2580ca000325aecf0a0562f8d9b6))
* filter prospects results after evaluation to match only to those that match minimum rating criteria ([fe3c7e3](https://github.com/b-partners/bpartners-api/commit/fe3c7e374001da482be6eac8686bfb7cd2ed28df))
* handle customer results after prospect evaluation ([3d3b1e0](https://github.com/b-partners/bpartners-api/commit/3d3b1e05ac023a0901bc5645d5ba3b80f99ef846))
* order prospect evaluation jobs by start datetime desc ([0acaea5](https://github.com/b-partners/bpartners-api/commit/0acaea5823146e083b7dca7d5b0848c0e4ec9af2))
* reformat prospect distance from intervention value inside mail sent after prospect evaluation ([fd3d245](https://github.com/b-partners/bpartners-api/commit/fd3d245300f8b345db3a966f378b0efac923d764))
* save and associate evaluated prospects to job ([eb63baf](https://github.com/b-partners/bpartners-api/commit/eb63bafd0db7918df61847940c9075c531aa1c96))
* set aws region in cd-network ([f03751f](https://github.com/b-partners/bpartners-api/commit/f03751f540c0ff105492ae91513ca3400a1c4cc8))


### Features

* read evaluation job details by id ([7e45dc9](https://github.com/b-partners/bpartners-api/commit/7e45dc9251e76cb49af28c1c665854eeb21142ac))
* run prospect evaluation job to convert calendar events ([fa12eaa](https://github.com/b-partners/bpartners-api/commit/fa12eaa2c3b1b5646f9a9c3b177a29d5be99eaf2))



# [0.31.0](https://github.com/b-partners/bpartners-api/compare/v0.29.1...v0.31.0) (2023-09-20)


### Bug Fixes

* add accept-header in prospecting-api evaluations spec ([693ef43](https://github.com/b-partners/bpartners-api/commit/693ef439d0573d37e5d79303fd5f0aedf3508ad4))
* add default value to date filters for calendar list ([0bcfbdd](https://github.com/b-partners/bpartners-api/commit/0bcfbdd3bff4fc515a48268062e505ab69466088))
* avoid NPE when getting calendar events without date-times ([05d6632](https://github.com/b-partners/bpartners-api/commit/05d66324c2acf4ab74e8d59be11c7dafacb339c2))
* avoid NPE when getting holidays events in calendar ([292d27a](https://github.com/b-partners/bpartners-api/commit/292d27a8ddd6b20ada0fa53596c6b1a28765e892))
* calendarEventIT getCalendars arguments ([63d7d5a](https://github.com/b-partners/bpartners-api/commit/63d7d5a0b7b99c7683b58f971bb8651438c1e544))
* get calendar events dispatched by calendar ([ad21d4e](https://github.com/b-partners/bpartners-api/commit/ad21d4e36bed434daa32cf6626b044a1a15368f0))
* persist calendar event when listing from google ([08a7f74](https://github.com/b-partners/bpartners-api/commit/08a7f74c2f686035150df7a0cc4cb6ccc80c2c6d))
* rename default products exported file correctly ([d6041c4](https://github.com/b-partners/bpartners-api/commit/d6041c4b5409ffb4fb150c7e63505da2b313155d))
* return google sheet access token validity details when exchanging code ([5afb349](https://github.com/b-partners/bpartners-api/commit/5afb349d76f6c3e8c2fdecbed093a98ca26a2636))
* return google sheet access token validity details when exchanging code ([4c3a55f](https://github.com/b-partners/bpartners-api/commit/4c3a55f36d2fe0b0dadcfc01f4c646f2bc461b9d))


### Features

* add sync status attribute to calendar event ([d742729](https://github.com/b-partners/bpartners-api/commit/d7427294b6a4eb346d80b15a5e86d34c9280b6c8))
* export all customers for a specific account ([9155298](https://github.com/b-partners/bpartners-api/commit/915529815675c07d98931d77412cdef72063729f))
* export all products for a specific account ([c211d66](https://github.com/b-partners/bpartners-api/commit/c211d668e6ee09f93e0f83f1cf12d43f090b6303))
* filter transactions by status ([cdd7711](https://github.com/b-partners/bpartners-api/commit/cdd7711a33d1181e900fc642dc1bf4f6451accea))
* get all account holders for authorized users ([f0ce059](https://github.com/b-partners/bpartners-api/commit/f0ce059d4a3905987bcec05429533f76d93aebb2))
* read prospect evaluation jobs for an account holder ([e218256](https://github.com/b-partners/bpartners-api/commit/e218256d326f9bc817adc834b333540178d6b471))



## [0.29.1](https://github.com/b-partners/bpartners-api/compare/v0.29.0...v0.29.1) (2023-09-13)


### Bug Fixes

* set prospect evaluations jobs and add calendar events - prospects conversion inside ([1ecdedc](https://github.com/b-partners/bpartners-api/commit/1ecdedca0c0306679e61ddafcd01894a10105b64))



# [0.29.0](https://github.com/b-partners/bpartners-api/compare/v0.28.1...v0.29.0) (2023-09-12)


### Bug Fixes

* **to-revert:** mock ssl context to allow communication with http expressif server ([6dbe876](https://github.com/b-partners/bpartners-api/commit/6dbe876b97df8afed76f11e21fbe71a3d2ab6bf5))


### Features

* configure google sheets OAuth2 ([d6a0683](https://github.com/b-partners/bpartners-api/commit/d6a0683aa2da4b1e3bf4229bf6bada7c095c96e6))
* evaluate prospect through google sheets ([e024294](https://github.com/b-partners/bpartners-api/commit/e0242942ac27ea6cac78963b01e5c9c65a612438))



## [0.28.1](https://github.com/b-partners/bpartners-api/compare/v0.28.0...v0.28.1) (2023-09-08)


### Bug Fixes

* transactionIT and prospectIT filters ([63ea268](https://github.com/b-partners/bpartners-api/commit/63ea2687c1879e999aca4eaff31c2b8108d901df))



