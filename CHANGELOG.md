# [0.30.0](https://github.com/b-partners/bpartners-api/compare/v0.29.1...v0.30.0) (2023-09-19)


### Bug Fixes

* add accept-header in prospecting-api evaluations spec ([693ef43](https://github.com/b-partners/bpartners-api/commit/693ef439d0573d37e5d79303fd5f0aedf3508ad4))
* add default value to date filters for calendar list ([0bcfbdd](https://github.com/b-partners/bpartners-api/commit/0bcfbdd3bff4fc515a48268062e505ab69466088))
* avoid NPE when getting calendar events without date-times ([05d6632](https://github.com/b-partners/bpartners-api/commit/05d66324c2acf4ab74e8d59be11c7dafacb339c2))
* calendarEventIT getCalendars arguments ([63d7d5a](https://github.com/b-partners/bpartners-api/commit/63d7d5a0b7b99c7683b58f971bb8651438c1e544))
* persist calendar event when listing from google ([08a7f74](https://github.com/b-partners/bpartners-api/commit/08a7f74c2f686035150df7a0cc4cb6ccc80c2c6d))
* rename default products exported file correctly ([d6041c4](https://github.com/b-partners/bpartners-api/commit/d6041c4b5409ffb4fb150c7e63505da2b313155d))
* return google sheet access token validity details when exchanging code ([5afb349](https://github.com/b-partners/bpartners-api/commit/5afb349d76f6c3e8c2fdecbed093a98ca26a2636))
* return google sheet access token validity details when exchanging code ([4c3a55f](https://github.com/b-partners/bpartners-api/commit/4c3a55f36d2fe0b0dadcfc01f4c646f2bc461b9d))


### Features

* add sync status attribute to calendar event ([d742729](https://github.com/b-partners/bpartners-api/commit/d7427294b6a4eb346d80b15a5e86d34c9280b6c8))
* export all customers for a specific account ([9155298](https://github.com/b-partners/bpartners-api/commit/915529815675c07d98931d77412cdef72063729f))
* export all products for a specific account ([c211d66](https://github.com/b-partners/bpartners-api/commit/c211d668e6ee09f93e0f83f1cf12d43f090b6303))
* filter transactions by status ([cdd7711](https://github.com/b-partners/bpartners-api/commit/cdd7711a33d1181e900fc642dc1bf4f6451accea))
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



# [0.28.0](https://github.com/b-partners/bpartners-api/compare/v0.27.0...v0.28.0) (2023-09-07)


### Features

* filter invoice by title ([a6f2052](https://github.com/b-partners/bpartners-api/commit/a6f20520d02fccb43ff1aca6c1afbe9ed9c0524f))
* filter prospect by name ([4ffd713](https://github.com/b-partners/bpartners-api/commit/4ffd71398a3f03712be6379944be2f666d1d214e))
* filter transaction by label ([7b85703](https://github.com/b-partners/bpartners-api/commit/7b85703245895236d7948dcd68355733b0373314))


### Reverts

* Revert "docs(api): refactor invoice filtering to filter by title and/or reference" ([f29ae5f](https://github.com/b-partners/bpartners-api/commit/f29ae5fda8d4deda3b6fa0110b839c529696d48b))
* Revert "docs(api): add query parameter for filtering invoice by title and/or reference " ([6f26cd7](https://github.com/b-partners/bpartners-api/commit/6f26cd7fdaab6fd0f597350632c891c5fa554a94))



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



