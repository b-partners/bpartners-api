# [0.7.0](https://github.com/b-partners/bpartners-api/compare/v0.6.0...v0.7.0) (2023-02-15)


### Bug Fixes

* set attachment content as base64 format ([d48d7e8](https://github.com/b-partners/bpartners-api/commit/d48d7e81cc1c217a1aa86e13cb2bc09736a76de5))


### Features

* add product created datetime for account ([3964bd8](https://github.com/b-partners/bpartners-api/commit/3964bd82e4cb4c430830fb5ef4bb0e31e3e3c27b))
* crupdate products ([a71fb1e](https://github.com/b-partners/bpartners-api/commit/a71fb1e545355bfc739fe3c1def4b1b59a41dd30))
* import customers from excel file ([bbf1cbb](https://github.com/b-partners/bpartners-api/commit/bbf1cbb366f6a42b1ba28e7ecafea8a0d20a9c17))
* import products from excel file ([6c742d9](https://github.com/b-partners/bpartners-api/commit/6c742d9cf59e3af0fab275ba2826aef669878852))
* invoice handles multiple payments ([566aea5](https://github.com/b-partners/bpartners-api/commit/566aea59cc14f33d1bd4ac989aa2f97fb8f96283))
* order products by criterias ([9176b78](https://github.com/b-partners/bpartners-api/commit/9176b785a1c58c3d2d285afe151d40cd5b0dcd84))



# [0.6.0](https://github.com/b-partners/bpartners-api/compare/v0.5.0...v0.6.0) (2023-02-09)


### Bug Fixes

* display HT instead of TTC when account holder is not subject to vat ([208521e](https://github.com/b-partners/bpartners-api/commit/208521eec25ed612dd32cb8f79bb27f0bfd3212e))
* format revenue target amount attempted as cent ([e30ca7f](https://github.com/b-partners/bpartners-api/commit/e30ca7f28e98b71268f428db9676c5ed83a34bc1))
* refresh transaction summary is invoked every hour ([d58e966](https://github.com/b-partners/bpartners-api/commit/d58e9663f7ae3db79d1d6e874e611212b8ba5c95))
* set product vat percent to 0 by default ([429293d](https://github.com/b-partners/bpartners-api/commit/429293d1d3817b6fe9cadd7df9903bca36bc6955))
* total without vat is shown in confirmed invoice when account holder not subject to vat ([64c1708](https://github.com/b-partners/bpartners-api/commit/64c1708af43e45bcf53e5bf2332f0ff759ab0473))
* total without vat is shown in draft invoice when account holder not subject to vat ([89a96e1](https://github.com/b-partners/bpartners-api/commit/89a96e1690a9273b10096c4171f0e7975faaf04b))


### Features

* add comment field to customer ([a0e8beb](https://github.com/b-partners/bpartners-api/commit/a0e8beba9b089853e225c46e41e34efd04f75704))



# [0.5.0](https://github.com/b-partners/bpartners-api/compare/v0.4.4...v0.5.0) (2023-02-02)


### Bug Fixes

* add input in Sched Depl compute ([#444](https://github.com/b-partners/bpartners-api/issues/444)) ([a746e1d](https://github.com/b-partners/bpartners-api/commit/a746e1dc6d03496367b0c9b157f4ad37b08371e8))
* attachments are persisted ([a424f5f](https://github.com/b-partners/bpartners-api/commit/a424f5fd5711fa30f9482304735c2da52b09196c))
* condition to run tests in cd-compute ([#446](https://github.com/b-partners/bpartners-api/issues/446)) ([f483ec4](https://github.com/b-partners/bpartners-api/commit/f483ec4df0b622a984ba336f725f2a4f16df1b6b))
* cron to run GHA on Madagascar Timezone ([#443](https://github.com/b-partners/bpartners-api/issues/443)) ([11b321a](https://github.com/b-partners/bpartners-api/commit/11b321a7c1aad2df270e6bd7e3a4adb1e2431ebd))
* event to trigger test step in cd-compute.yml ([#439](https://github.com/b-partners/bpartners-api/issues/439)) ([dee247a](https://github.com/b-partners/bpartners-api/commit/dee247a57782bc3b66beb8c816eccb7c9e465949))
* reference input in workflow_call ([#445](https://github.com/b-partners/bpartners-api/issues/445)) ([9a6ff8f](https://github.com/b-partners/bpartners-api/commit/9a6ff8f0ec00e22d8115a7a8f8def87ee6596dbc))
* run tests when trigger is not 'schedule' and run_test different from false ([#448](https://github.com/b-partners/bpartners-api/issues/448)) ([11380c2](https://github.com/b-partners/bpartners-api/commit/11380c23851a6caad0892c9a8a0de56e30bde0d0))
* Sched Depl compute workflow path ([#438](https://github.com/b-partners/bpartners-api/issues/438)) ([5dfd629](https://github.com/b-partners/bpartners-api/commit/5dfd6299df6760baba1f7f3ece7e5280b5842c9b))
* validity date attribute compatibility is transitional during invoice crudpate ([f649991](https://github.com/b-partners/bpartners-api/commit/f649991be3f068035e6136301eeae261181d70cc))


### Features

* add attachments to mail ([2f94cb0](https://github.com/b-partners/bpartners-api/commit/2f94cb0ec3014e91f5db3f68f9a9b8ed656a9f27))
* add revenue targets to account holder ([dd23285](https://github.com/b-partners/bpartners-api/commit/dd23285596bc3e8e7ade46eaf779d42c5bdbf292))
* handle multiple payments initiation ([b2c5e94](https://github.com/b-partners/bpartners-api/commit/b2c5e94a083b07fd08ad9c4ca4e3f8e99d189e8b))



## [0.4.4](https://github.com/b-partners/bpartners-api/compare/v0.4.3...v0.4.4) (2023-01-26)


### Bug Fixes

* ignore git_push.sh file when publishing the codeartifact client ([#432](https://github.com/b-partners/bpartners-api/issues/432)) ([dba71b2](https://github.com/b-partners/bpartners-api/commit/dba71b23bf1a212fab4c38f45a058ee0f12f952b))



## [0.4.3](https://github.com/b-partners/bpartners-api/compare/v0.4.2...v0.4.3) (2023-01-26)


### Bug Fixes

* command pattern on the content to append to .npmignore ([#431](https://github.com/b-partners/bpartners-api/issues/431)) ([564d2b1](https://github.com/b-partners/bpartners-api/commit/564d2b12104555db0915dfff0b8ecf6faf02e696))



## [0.4.2](https://github.com/b-partners/bpartners-api/compare/v0.4.1...v0.4.2) (2023-01-26)


### Bug Fixes

* echo command to append to .npmignore file ([#430](https://github.com/b-partners/bpartners-api/issues/430)) ([f192f24](https://github.com/b-partners/bpartners-api/commit/f192f2493c883fe9384b37f5e8dca2acbe658815))



## [0.4.1](https://github.com/b-partners/bpartners-api/compare/v0.4.0...v0.4.1) (2023-01-26)


### Bug Fixes

* accept null phone number but validate if not null when creating preUsers ([63f1d93](https://github.com/b-partners/bpartners-api/commit/63f1d9313bd5e6bbf19e16457fcfb0209ffc4b34))
* cpu configurations for ecs ([#427](https://github.com/b-partners/bpartners-api/issues/427)) ([257cd32](https://github.com/b-partners/bpartners-api/commit/257cd326dc112c8f8f807520e6e067630465b9ad))
* delete redundant transactions and associated categories from database ([eb02d9f](https://github.com/b-partners/bpartners-api/commit/eb02d9fa78283e3989a24aa1bafaaaa96a2e032f))
* ecs configurations ([#426](https://github.com/b-partners/bpartners-api/issues/426)) ([0247b79](https://github.com/b-partners/bpartners-api/commit/0247b79149bd03b70977f1c41572d75c9ee78853))
* match invoice sending date and today's date ([466a980](https://github.com/b-partners/bpartners-api/commit/466a980b67da2467e7448358f19f04a1d480c695))
* modify package.json, tsconfig.json and .npmignore before publishing react-client to codearifact ([#423](https://github.com/b-partners/bpartners-api/issues/423)) ([45cb68b](https://github.com/b-partners/bpartners-api/commit/45cb68b1de2700ffc3300ed8acabd3b6955e800b))
* set vat to zero when account holder is not subject to vat ([5f605ff](https://github.com/b-partners/bpartners-api/commit/5f605ff50a289f07128c34a15d519ece58837c20))
* tasksDesiredCount type ([#425](https://github.com/b-partners/bpartners-api/issues/425)) ([7194009](https://github.com/b-partners/bpartners-api/commit/7194009cacf4571f4daf07c64dfe34bc4af93328))
* **to-drop:** toPayAt and validityDate have same values for draft invoice ([1fb0e55](https://github.com/b-partners/bpartners-api/commit/1fb0e552c64e24cb7d891c38f4feb76d6d11e4f1))
* transaction mapper use the provided filtered account ID during mapping ([6129753](https://github.com/b-partners/bpartners-api/commit/612975348f167da1b285d86bd88eb37779612cbe))
* update uses version in get-configuration in cd-compute.yml ([#424](https://github.com/b-partners/bpartners-api/issues/424)) ([ea528ef](https://github.com/b-partners/bpartners-api/commit/ea528efab83060206cdcc0b54e7a3b935e6921ee))



# [0.4.0](https://github.com/b-partners/bpartners-api/compare/v0.2.4...v0.4.0) (2023-01-20)


### Bug Fixes

* customize delay in payment allowed and delay penalty percent for invoice ([778da01](https://github.com/b-partners/bpartners-api/commit/778da018216aa627190d1b568687042ea6b11600))
* delete duplicated products ([f594dc9](https://github.com/b-partners/bpartners-api/commit/f594dc97e55648277dcf9367486b628eac5d68f5))
* draft invoice have validity date and only confirmed have payment limit date ([4ffd682](https://github.com/b-partners/bpartners-api/commit/4ffd682444fa56dfb82841d70b47f7d03f6d908d))
* draft product description wraps if too long and footer page is repeated correctly ([e641492](https://github.com/b-partners/bpartners-api/commit/e6414924ab780dc0aa815d74a13b3a10879f0bb8))
* drop business activity template fk constraint only if exists ([f0abc12](https://github.com/b-partners/bpartners-api/commit/f0abc1281b0f6b8f0e3654f0bf9649df451bf008))
* file upload is synchronous to avoid event bridge file size limit exception ([ce577b3](https://github.com/b-partners/bpartners-api/commit/ce577b33c214b6483bc473b15137e2f21625f1fb))
* fintecture and swan project token are refreshed automatically ([53e3036](https://github.com/b-partners/bpartners-api/commit/53e3036e584fe9dde590e929175c6ae7db6a56af))
* improve invoice performance issue ([752a7c8](https://github.com/b-partners/bpartners-api/commit/752a7c88eb442a5c2cd25bb248a36d2cddf3afaa))
* map sending date as validy date for draft and proposal invoice ([6f150a7](https://github.com/b-partners/bpartners-api/commit/6f150a7e9a33193382e8f9df03689899fa2d1422))
* multiple account holders handle business activities update ([d3080e5](https://github.com/b-partners/bpartners-api/commit/d3080e5c09cb140cadb5711908ad6135ba806793))
* remove duplicated business activity template ([1b9f137](https://github.com/b-partners/bpartners-api/commit/1b9f137ffc99862b66b5f8246814bfe758a367b8))
* rename invoice reference prefix when draft and proposal status ([28cc855](https://github.com/b-partners/bpartners-api/commit/28cc855cd94fa0c00427cd598f5dd2f5d48db013))
* same invoice with different status handle null reference ([e3882f9](https://github.com/b-partners/bpartners-api/commit/e3882f96bb0e1b49c4003076ff159a0a349d328a))


### Features

* account holder may be suject to vat ([e3c21e2](https://github.com/b-partners/bpartners-api/commit/e3c21e2223695eb04386630214cba5f4d4f4f2ab))
* add transactions annual summary ([ba2e527](https://github.com/b-partners/bpartners-api/commit/ba2e52779eea86166d141ae91953bd22622bffb4))



## [0.2.4](https://github.com/b-partners/bpartners-api/compare/v0.2.3...v0.2.4) (2023-01-07)


### Bug Fixes

* rename bpartners-client to bparnters-react-client with proper API_URL ([0ec9baa](https://github.com/b-partners/bpartners-api/commit/0ec9baaa278b806d2f6a1f54d1b251195c88382f))



## [0.2.3](https://github.com/b-partners/bpartners-api/compare/v0.2.2...v0.2.3) (2023-01-07)


### Bug Fixes

* do not change enum capitalization in ts-client ([f9f04d2](https://github.com/b-partners/bpartners-api/commit/f9f04d2d00f2ae4a4c0f64596980c11a90ecc89d))



