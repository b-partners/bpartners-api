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



## [0.2.2](https://github.com/b-partners/bpartners-api/compare/v0.2.1...v0.2.2) (2023-01-07)


### Bug Fixes

* map dates correctly in ts-client ([1b98dde](https://github.com/b-partners/bpartners-api/commit/1b98dde08caab7cacdc8d34fba66a7f1ae7f14e0))



## [0.2.1](https://github.com/b-partners/bpartners-api/compare/v0.2.0...v0.2.1) (2023-01-07)


### Bug Fixes

* ts-client generation withSeparateModelsAndApi is broken ([bf51b2e](https://github.com/b-partners/bpartners-api/commit/bf51b2e5de45e000d918798a293d2a3530847705))



# [0.2.0](https://github.com/b-partners/bpartners-api/compare/da05902300b104b8ddbdc5669914d23102600953...v0.2.0) (2023-01-06)


### Bug Fixes

* accept unique unverified account holder when none is still verified ([6d4090a](https://github.com/b-partners/bpartners-api/commit/6d4090acf557ebd65f23c762bbe030e37143b5c9))
* account and account holder throws 50x exception instead of indexOutOfBounds when not fetched ([055db4f](https://github.com/b-partners/bpartners-api/commit/055db4fd4ee4339bca52847e42a2c4a0466bf08d))
* account holder is persisted if it is not, when its exists in swan ([18a3a00](https://github.com/b-partners/bpartners-api/commit/18a3a00f808e4ca287ba03def7a4f14874c99c06))
* account status info has default constructor for deserialization ([32aebd8](https://github.com/b-partners/bpartners-api/commit/32aebd80c7dd2c9de284b2125964f2fff18356f0))
* add additional HTML tags for custom email body ([8233125](https://github.com/b-partners/bpartners-api/commit/82331252f74848e3470955aa9363f75bb21274a3))
* add global relaunch configuration for invoices of an account ([8d6962e](https://github.com/b-partners/bpartners-api/commit/8d6962ecc6f09aae69f4362db348cddc824a8a8a))
* add isOther property to transaction category ([ee151be](https://github.com/b-partners/bpartners-api/commit/ee151be6e98b02ceebb240ab7dba0e4fcd95a982))
* add new REJECTED transaction status ([c9bbb3c](https://github.com/b-partners/bpartners-api/commit/c9bbb3cdedab886a5f02bd907d22f707342c0e47))
* add specific relaunch configuration for each invoice ([ea6a9c8](https://github.com/b-partners/bpartners-api/commit/ea6a9c82afba20ee9a5324eb7e34846c7046d226))
* add transaction category template to old values without it ([b62fadf](https://github.com/b-partners/bpartners-api/commit/b62fadfc8eb2902f7a84f3b9c2a536fc722e5c24))
* add upcoming transaction status ([588145e](https://github.com/b-partners/bpartners-api/commit/588145ed63d41efdf6aa1d24c0b2d74df21c788a))
* allow <del> HTML tags for custom email body formatting ([97902dc](https://github.com/b-partners/bpartners-api/commit/97902dc6783dc45d5cc6745da13dc62089b42dfb))
* auth return redirection url as string only ([4d3b771](https://github.com/b-partners/bpartners-api/commit/4d3b771e7257b8375fecfa4f3e543e12b22be38e))
* authentication works with swan api ([6cbb42a](https://github.com/b-partners/bpartners-api/commit/6cbb42a9d9938ec4a075738533f93567babb04b2))
* can remove products from invoice ([6f131bc](https://github.com/b-partners/bpartners-api/commit/6f131bcec421c1c30c2bba28fdea048c790ebbfc))
* category is not always null for transaction ([2265916](https://github.com/b-partners/bpartners-api/commit/22659166961e66027e49caf10bd7c05e92bea203))
* category is nullable for transaction ([2e661cb](https://github.com/b-partners/bpartners-api/commit/2e661cbc6d675dd5bc0d0539a5b6b3a2ec8038f3))
* change fintecture payment request endpoint to /request-to-pay and sign request ([b30bf28](https://github.com/b-partners/bpartners-api/commit/b30bf28aefc658151f0c0488ef214a5de5fb1c61))
* change swan project parameter name in ssm ([38e6dbb](https://github.com/b-partners/bpartners-api/commit/38e6dbb381b362dc14a4d18b97956e84482255e0))
* check if category exists in rest mapper ([86e265b](https://github.com/b-partners/bpartners-api/commit/86e265b4d3fc8f0305df44c5bde50416e75f4deb))
* check if project has projectProperty or not in generateTsClient ([cd50173](https://github.com/b-partners/bpartners-api/commit/cd50173d5a7552349f12bd10a5588c9774159b8b))
* check invoice customer ID on crupdate ([8266b91](https://github.com/b-partners/bpartners-api/commit/8266b91e9de6d324d225fa2eacc0b9839baa3787))
* check invoice reference before crupdate ([9473344](https://github.com/b-partners/bpartners-api/commit/9473344e1237c5f1336c2c6d199a1b2631556ef7))
* check invoice reference unicity with status ([5a1a3ea](https://github.com/b-partners/bpartners-api/commit/5a1a3eab9556b283ccb84ed9ae85c2e88a359a34))
* check legal file approval in authProvider except for its endpoints ([4568fa9](https://github.com/b-partners/bpartners-api/commit/4568fa98ee4181a5e684cb601c7c18c3061bd12f))
* check products and amount before changing invoice status to PROPOSAL or CONFIRMED ([94bfc1d](https://github.com/b-partners/bpartners-api/commit/94bfc1d5c9627a5ac51a19108466595d79753b5e))
* config delay event poller scheduler to 0.2s and wait time to 0s ([54607b0](https://github.com/b-partners/bpartners-api/commit/54607b03a16f8da3f137227ce747e010309ef7f6))
* create new products does not need an invoice ([d7ccbfd](https://github.com/b-partners/bpartners-api/commit/d7ccbfddc2515ee0e6f84f346083ae5932c21466))
* creating products is not attached to invoice ([7eb9435](https://github.com/b-partners/bpartners-api/commit/7eb94352ff67b7cc99e083aae6c00affb358c784))
* crupdate invoice accepts null attributes except status ([697d0e9](https://github.com/b-partners/bpartners-api/commit/697d0e99cb47f8bec7d1f05ec91c24c726f74f95))
* custom business activities are chosen when set for account holders ([9066891](https://github.com/b-partners/bpartners-api/commit/9066891a4c18669b89b2444b7e91e0fd108caa27))
* do not send email verification during customer creation ([f0bbae8](https://github.com/b-partners/bpartners-api/commit/f0bbae8344e0a2899cdfa3386ca6bb169627410e))
* download file throws the appropriate bad request exception ([9a80897](https://github.com/b-partners/bpartners-api/commit/9a80897687c83bf8779017ba62659232b42b786c))
* draft invoice is not sent by email ([500c9ab](https://github.com/b-partners/bpartners-api/commit/500c9abca61f0665a99b3c457532e34d8f59d66e))
* edit invoice without logo ([4487dbe](https://github.com/b-partners/bpartners-api/commit/4487dbe6e917708006c8beab9c0155b8a4b60b30))
* editing customer does not impact existing invoices ([116beba](https://github.com/b-partners/bpartners-api/commit/116beba0a97f6531b8ff1582f1f56bad04a30524))
* email is mandatory for pre-registration ([f70da6a](https://github.com/b-partners/bpartners-api/commit/f70da6adeec54e4f78256e170aeb35f485c1437a))
* every accounts have the same marketplaces and default values are migrated in flyway ([526e85b](https://github.com/b-partners/bpartners-api/commit/526e85b72985e59e9260a57e6199396b7e816a6a))
* file download is not publicly accessible anymore ([2190d0e](https://github.com/b-partners/bpartners-api/commit/2190d0e7e62339cb7439d2aaa558d240491b84c2))
* file ID is returned every invoice crupdate request ([3575a9c](https://github.com/b-partners/bpartners-api/commit/3575a9c9de0321ee29d75768b81b12b0164ad09d))
* fileID is correctly mapped for invoice file upload ([24d2ba3](https://github.com/b-partners/bpartners-api/commit/24d2ba3d37a480d76c592aabfde8385950dc9346))
* filter preUsers by criteria ([9808f32](https://github.com/b-partners/bpartners-api/commit/9808f3247216b33731d0d4f397040e98e9fc6a06))
* finding transaction category by transaction is transactional ([aac3e48](https://github.com/b-partners/bpartners-api/commit/aac3e48603ac6b8ad781153408e7671b033bd147))
* generated invoices pdf load styles ([d200a29](https://github.com/b-partners/bpartners-api/commit/d200a29584f11e9b733a8e99e1de5e5abb8065cd))
* get accountHolders for authenticated only ([9fb0e34](https://github.com/b-partners/bpartners-api/commit/9fb0e34f1e2108bd89947fff55919772a78cca8c))
* get all invoices does not throw duplicated customer exception ([887db44](https://github.com/b-partners/bpartners-api/commit/887db44b5dcb895ae0274c15fbfe798be4489e7c))
* GET and POST pre-registrations work correctly ([96fd70c](https://github.com/b-partners/bpartners-api/commit/96fd70c28ee6814007458b5cb6d59aa53f16c82e))
* get transactions categories with correct values ([5901774](https://github.com/b-partners/bpartners-api/commit/59017749d295cab08e262104f57e95b5306e7f6f))
* GET whoami and GET /users/id are not filtered by the legal file check ([b5e3916](https://github.com/b-partners/bpartners-api/commit/b5e3916d33268e7a521e50d37e05d87aed9d2d5d))
* guess media type from uploaded fily type only ([6f2905e](https://github.com/b-partners/bpartners-api/commit/6f2905edf71b609ab3fca14835a56c9d235f9bd4))
* handle default exception to api exception ([90ed9a1](https://github.com/b-partners/bpartners-api/commit/90ed9a1d40a9776958b8daae0f7d69047425468f))
* invoice and file asynchronous upload works correctly ([32f2111](https://github.com/b-partners/bpartners-api/commit/32f21118d18598539dc3bfb468495e9aefdd5073))
* invoice created datetime is mapped before saving ([45d3cae](https://github.com/b-partners/bpartners-api/commit/45d3caed51920af315224fe54d2353da36ef579a))
* invoice crupdated does not generate new fileID before upload ([9f39657](https://github.com/b-partners/bpartners-api/commit/9f39657a2f74a31b3304d5226b2381e1f8d5bfa1))
* invoice customer created datetime is correctly set ([05fc448](https://github.com/b-partners/bpartners-api/commit/05fc448a83cd4457f6ef8adf1f8edca913643ebf))
* invoice file ID is null during crupdate and updated in event crupdated service ([f8cb4ad](https://github.com/b-partners/bpartners-api/commit/f8cb4ad37d7a3b677bce6dc6b6b2a72c11f804f3))
* invoice generated pdf load style ([a3ce411](https://github.com/b-partners/bpartners-api/commit/a3ce41137e260dfd563bf7e837e420bbd1551dff))
* invoice pdf generation is treated both synchronous and asynchronous ([f608169](https://github.com/b-partners/bpartners-api/commit/f60816909368d149501aaa029c6f7bfaa5c1761a))
* invoice product has ID ([e97cbe9](https://github.com/b-partners/bpartners-api/commit/e97cbe9de9eaa03fa197806baecdb09eae969bf3))
* invoice reference unicity is not checked while status is draft ([3297db7](https://github.com/b-partners/bpartners-api/commit/3297db7a48d8d4493701fa0d400c8ddb5fe7a9b9))
* invoice sending date can be today ([d8f9937](https://github.com/b-partners/bpartners-api/commit/d8f993722410ba6ffa4a6eae10cda1bb532e388a))
* invoice with null reference is not checked ([114c59a](https://github.com/b-partners/bpartners-api/commit/114c59a4203085e37fcb552f858324fb3a768e75))
* invoices are orderer by creation datetime when listing ([008da2e](https://github.com/b-partners/bpartners-api/commit/008da2ed89c1e8a716be3571e36847b12f2fbdab))
* isOther is mapped in transaction category mapper ([9c257fd](https://github.com/b-partners/bpartners-api/commit/9c257fd6a8d6b3176e8533ec409a63c4245ec2a8))
* onboarded user can approve legal file while it is not ([36a7bf7](https://github.com/b-partners/bpartners-api/commit/36a7bf7a61174d253a4b5259b24b452c8cb65c4a))
* onboarded user can approve legal file while it is not ([75c9b46](https://github.com/b-partners/bpartners-api/commit/75c9b4693d2e5500ced72cf7b876b67327f3015b))
* one account can support multiple account holder while only one is verified ([90cfb70](https://github.com/b-partners/bpartners-api/commit/90cfb70d61e5aa4cdf32457149f62a61c1820012))
* only description and unit price are mandatory when creating products ([6135bbd](https://github.com/b-partners/bpartners-api/commit/6135bbdbbd689c674fb363d61de6816a6de49a08))
* only email is mandatory for preUser ([e73e92c](https://github.com/b-partners/bpartners-api/commit/e73e92c8cefe61a0f94c05ad25b1c0e386c4af19))
* parse invoice template correctly for generating pdf ([b554167](https://github.com/b-partners/bpartners-api/commit/b554167bbe9ca23b2a972a5ec87102763d2e276a))
* payer name is correctly mapped instead of payer email ([7ef7d73](https://github.com/b-partners/bpartners-api/commit/7ef7d738b9118e3af53e89916bb14654ce251420))
* persist user if not exist when requesting for the first time ([00422fa](https://github.com/b-partners/bpartners-api/commit/00422fae52f3125d2677244c88c1c07756ccd679))
* phoneNumber is encoded in auth initiation URL ([3caa095](https://github.com/b-partners/bpartners-api/commit/3caa095e421b1fcab40cae10e07a3ce9af7993e5))
* product vat percent in generated pdf is shown in cents ([0c088c6](https://github.com/b-partners/bpartners-api/commit/0c088c671f9d228b93e11763c3d423f97ebfa64a))
* products invoices are persisted correctly ([5020ac7](https://github.com/b-partners/bpartners-api/commit/5020ac728e67b2dd6c94a0ed2f875a16f8ca29aa))
* project tokens are refreshed before they are used into other scheduled tasks ([2234e02](https://github.com/b-partners/bpartners-api/commit/2234e02d36aee8dd415613b733a7e5396f598b7e))
* project tokens are refreshed on project start ([5d2f6b1](https://github.com/b-partners/bpartners-api/commit/5d2f6b19b4c6d633f0b459e6788f13e2be575910))
* proposal invoice are set to confirmed status after confirmation ([48f9780](https://github.com/b-partners/bpartners-api/commit/48f9780c8dd3568e126e7867792780c388959bbe))
* read all transaction categories without userDefined filter ([54a9624](https://github.com/b-partners/bpartners-api/commit/54a9624b763284a0008c1939d639bec7eddda7af))
* remove redundant mapper for transaction category vat ([12f69a5](https://github.com/b-partners/bpartners-api/commit/12f69a5df48c96c3ab0ca4f43944d9496ed6c494)), closes [#326](https://github.com/b-partners/bpartners-api/issues/326)
* repair ci & cd ([ba01e9b](https://github.com/b-partners/bpartners-api/commit/ba01e9b7bc790beb726260bff381042652b17fcb))
* retrieve account holder vat number from swan ([a65693d](https://github.com/b-partners/bpartners-api/commit/a65693d2d634ceda1a3956663685d5a293d2acf3))
* return category template ID when category is null ([9cab263](https://github.com/b-partners/bpartners-api/commit/9cab263a436587bc5818c3d43b817408678d609f))
* return marketplace phoneNumber ([0e2fc06](https://github.com/b-partners/bpartners-api/commit/0e2fc06b35238ac71f96849af9eefa5c786249d7))
* return onboarding url as string ([0cbd223](https://github.com/b-partners/bpartners-api/commit/0cbd2236dd6f434602151d5d48c605fd98ae52d2))
* return unknown transaction status when mapped status is unknown ([e292e8e](https://github.com/b-partners/bpartners-api/commit/e292e8ef4a4de13612a231b8de2fef2730ab739f))
* returns correct count when getting transaction categories ([58d853e](https://github.com/b-partners/bpartners-api/commit/58d853e8d4de3326a0660687385ce8f161887674))
* revert account holder schema for compatibility ([51beb0f](https://github.com/b-partners/bpartners-api/commit/51beb0f025ccc2a1ca3ce9f9620ee8054bb834d8))
* schedule the project token refresh to 45 minutes intervals ([9f18bb0](https://github.com/b-partners/bpartners-api/commit/9f18bb06af5c45505d665bb5f9fe8b28537254ba))
* script paths ([b5fab3b](https://github.com/b-partners/bpartners-api/commit/b5fab3becfe5520788fe0f2963222bbc042f6b6e))
* send verification email to customer after creation ([17ecdd6](https://github.com/b-partners/bpartners-api/commit/17ecdd6b139dd7a63350e52d9f023462229ea2d9))
* set default fraction parsing value to zero ([95a4237](https://github.com/b-partners/bpartners-api/commit/95a4237ea58a07ea39e9a3dc2f15408586b19999))
* set invoice updated_at instant manually ([033848f](https://github.com/b-partners/bpartners-api/commit/033848ffdb89c93c6e44a5818c990216df049f64))
* set the correct key for file upload ([341dd8c](https://github.com/b-partners/bpartners-api/commit/341dd8cd1808f736441f617283cff5b0da8976b3))
* set user definition for transaction categories ([bbb30ef](https://github.com/b-partners/bpartners-api/commit/bbb30efc412140e8132b5e731466d4b3ea5709e0))
* skip the invoice crupdated service to avoid data inconsistency ([93b1926](https://github.com/b-partners/bpartners-api/commit/93b1926b1ef3a04874d07f10062951f3ae44920e))
* some attributes are not mandatory when creating customer ([7041069](https://github.com/b-partners/bpartners-api/commit/704106998fe83e3c0b037c48b44dd35502651a31))
* ssmClient bean never closes ([f434c2d](https://github.com/b-partners/bpartners-api/commit/f434c2dc11320ae6f98383a0b11098ad784a56b3))
* support multiple verified account holders when different accounts own it ([5ca7854](https://github.com/b-partners/bpartners-api/commit/5ca7854c29442869f781666d73ea61d79303db9e))
* throw exception during download when fileID does not match with any account ([9875a57](https://github.com/b-partners/bpartners-api/commit/9875a574b9b9179fdd6124efff5bcc72be69288c))
* throw exception when customer is not found ([77de9b2](https://github.com/b-partners/bpartners-api/commit/77de9b23824b9fe83618600c764ae20efa063282))
* timestamp does not have time zone ([3e1dd25](https://github.com/b-partners/bpartners-api/commit/3e1dd25a2024362ccc463949c8720a2acfd6e7c6))
* transaction categories filtered by unique have the most recent ID ([f1ca4ab](https://github.com/b-partners/bpartners-api/commit/f1ca4ab859b435a6b2af5e2d3aa15557d462c737))
* transaction categories filtered by unique only have ID ([1459698](https://github.com/b-partners/bpartners-api/commit/14596983fe3478274ea0feceb09cfc3d5a348d5b))
* transaction category count is filtered by dates ([8e48ee5](https://github.com/b-partners/bpartners-api/commit/8e48ee54020d5fe8ab3a10e793b39b1cb9bd086a))
* transaction category template list does not have a duplicate element ([cabc9d4](https://github.com/b-partners/bpartners-api/commit/cabc9d432e2b38af3ed3bdadfd538b6d4c65bc6c))
* transactions summaries scheduler is transactional ([39aca6e](https://github.com/b-partners/bpartners-api/commit/39aca6e11c47088b6975643f0df50a7fd25af899))
* transactions summaries scheduler update the current year summary ([ee19a4b](https://github.com/b-partners/bpartners-api/commit/ee19a4bfbce4687db536bb69323d70d6d5fd35d9))
* transactions summary are specific for each account ([eae4329](https://github.com/b-partners/bpartners-api/commit/eae4329e0cb407981769e979a56f21c6b704d5a7))
* upload file and download file are now validated ([01c6ee6](https://github.com/b-partners/bpartners-api/commit/01c6ee64ca79d0a04e5dbc07efa67e8bc6f0d850))
* use correct variables names for swan account and transaction queries ([4ea1d3f](https://github.com/b-partners/bpartners-api/commit/4ea1d3fcab5bf4e4d4f5b2ed78a367a28b7634aa))
* user can have several accounts if only one is active ([293e215](https://github.com/b-partners/bpartners-api/commit/293e215a0ea14c59064a92f9d24fabe7e75eab01))
* user is persisted automatically during the first login ([360be98](https://github.com/b-partners/bpartners-api/commit/360be98c6cee52d90f13d9948f9bf44e637ab2ed))
* user logo file ID is persisted when user logo is updated ([c34f945](https://github.com/b-partners/bpartners-api/commit/c34f945ca7d1b63ca228be0ebd6f194a19fde26f))
* users have identification status attributes ([2dcbe4a](https://github.com/b-partners/bpartners-api/commit/2dcbe4afdeabfb30397201dce373bf3a3ccce4d2))


### Features

* add account holder verification status ([505205d](https://github.com/b-partners/bpartners-api/commit/505205d429bdbc3428a2f6260ce11f6804652b28))
* add account status ([0710db2](https://github.com/b-partners/bpartners-api/commit/0710db22680465e796db040978c419f37ea16b15))
* add additional informations for customer ([1699a61](https://github.com/b-partners/bpartners-api/commit/1699a61dd28c5386890160ccf709e1b0845c0cc9))
* add attachment file ID to invoice relaunch ([92c9717](https://github.com/b-partners/bpartners-api/commit/92c97174ba2ebb0bd47f99b913e4ba9964aea8f9))
* add avalaible balance for account ([6dc691d](https://github.com/b-partners/bpartners-api/commit/6dc691d915c6ae6b67a0a6a93b09c9cfe0158bde))
* add description and comment to transaction categories ([fd2c311](https://github.com/b-partners/bpartners-api/commit/fd2c3117eefb97c18182a7711004743d88c95435))
* add invoice.metadata ([526e324](https://github.com/b-partners/bpartners-api/commit/526e324aad0b806c162880753969c84170aa6845))
* add SES Full access in task role ([fcedd25](https://github.com/b-partners/bpartners-api/commit/fcedd2513438840e1d99c4b5843b94afa49f583b))
* add status to transaction ([5ab2566](https://github.com/b-partners/bpartners-api/commit/5ab25669d6b25879f4dcd926a3bba115bf5b001b))
* add transaction categories count filtered by dates ([3ff1aef](https://github.com/b-partners/bpartners-api/commit/3ff1aef798aa3d84ffe827855ac3b53dca41312f))
* add unit price with vat for product ([bd5ab16](https://github.com/b-partners/bpartners-api/commit/bd5ab165f05ebd4ae7eb291861ead2a4df6b9d8b))
* **api:** get marketplaces for an account ([b4d078c](https://github.com/b-partners/bpartners-api/commit/b4d078cd23faad0f9fad02bcd5afcf560885c9a8))
* **api:** relaunch message frequency ([844a7be](https://github.com/b-partners/bpartners-api/commit/844a7be3fba5d7222f933356e15c810646bf115d))
* auth redirection to swan ([4840c7b](https://github.com/b-partners/bpartners-api/commit/4840c7b969a9b6f31ee396c9bbdd7e2556e6d0bc))
* authentication with mocked Swan API ([da05902](https://github.com/b-partners/bpartners-api/commit/da05902300b104b8ddbdc5669914d23102600953))
* categorize transaction ([bc033a3](https://github.com/b-partners/bpartners-api/commit/bc033a3d41cbf0345c1e1ccba762e83b4a6ae041))
* change invoice status to PAID ([e1aa030](https://github.com/b-partners/bpartners-api/commit/e1aa030e60f19f0044d0d32747ac8484c85350ed))
* compute invoice values and add its validator ([fdb7e4e](https://github.com/b-partners/bpartners-api/commit/fdb7e4e4f88dbe9a26b386d2b1528454ba7b31a7))
* create and get users pre-registrations ([0098ba0](https://github.com/b-partners/bpartners-api/commit/0098ba02b869cfba99c99c36ec5e33f06ef30fad))
* create and read account customers ([72a6c57](https://github.com/b-partners/bpartners-api/commit/72a6c571ef992e72630d4796ea37a86f5e600261))
* create and read transaction category type ([4ca635f](https://github.com/b-partners/bpartners-api/commit/4ca635fdbf746861265275efafc1caff0dc41557))
* create products for an invoice ([460e125](https://github.com/b-partners/bpartners-api/commit/460e1251bf2601585b1223a64fcf7ae0f71e6503))
* crupdate invoice without computed value ([79c6081](https://github.com/b-partners/bpartners-api/commit/79c60810f3e370c7d2384e7ddf1058a69cdd1f19))
* email generated invoice when proposed or confirmed ([ab3f0fd](https://github.com/b-partners/bpartners-api/commit/ab3f0fd2d555abb80dd65016cb833989d360314c))
* filter customers by name ([a819807](https://github.com/b-partners/bpartners-api/commit/a8198078280c884620ea0e6e58a69312febb2a8f))
* filter invoice by status ordered by created datetime desc ([736e60c](https://github.com/b-partners/bpartners-api/commit/736e60cb5f80af315db9c387f181c9ef0e77c31c))
* filter products by description ([9d5886e](https://github.com/b-partners/bpartners-api/commit/9d5886ef245d58eed40d54235bf379e87693bb02))
* generate invoice pdf ([d1270d9](https://github.com/b-partners/bpartners-api/commit/d1270d9ab73693ad9b5ad960a730bc445d675eb7))
* get all invoices for an account ([6694951](https://github.com/b-partners/bpartners-api/commit/66949514415594a647de61198812cd3c565edcc1))
* get and approve user legal files ([15c2012](https://github.com/b-partners/bpartners-api/commit/15c20125b5d3d666fae24fa895a0dfca16ab2ffa))
* get and upload file ([c6994bf](https://github.com/b-partners/bpartners-api/commit/c6994bf23ca52622c048c2341a1b791acea0cea9))
* get authenticated user account ([d3eafa5](https://github.com/b-partners/bpartners-api/commit/d3eafa57e366a89183279fad621a7c60496285fc))
* get authenticated user accountHolder ([0bd3564](https://github.com/b-partners/bpartners-api/commit/0bd35642eb524e106356116f127cfd5b16d6806d))
* get authenticated user transactions ([b06654a](https://github.com/b-partners/bpartners-api/commit/b06654a9bcd92bb103fbb49f1eb15a59426d2c49))
* get company business activities ([8e983ee](https://github.com/b-partners/bpartners-api/commit/8e983eeef50438abc3ce9d7e32975daf0131d4ab))
* get transactions categories filtered by type ([6df212e](https://github.com/b-partners/bpartners-api/commit/6df212ea2e42794802298a791ad103f5b57aaf73))
* get transactions summary of an account ([7920141](https://github.com/b-partners/bpartners-api/commit/7920141e825448363aff4d0d946d93577e12f0f4))
* get user by id ([88ee957](https://github.com/b-partners/bpartners-api/commit/88ee9578c1ec7bac4e5e66989bed458246300719))
* historize invoice relaunch ([22ec78c](https://github.com/b-partners/bpartners-api/commit/22ec78cb8bbfc1e6a469275ccdd049992a6b3354))
* integrate preUsers to sendinblue ([a2c4c43](https://github.com/b-partners/bpartners-api/commit/a2c4c435dc1d4bd7f5f2434f13b5b7aa485d6c8c))
* invoice can have comments ([dd72053](https://github.com/b-partners/bpartners-api/commit/dd72053766edabbb6c7179fd5643a99d787eec54))
* invoice relaunch email object and body are persisted ([b9dfd5c](https://github.com/b-partners/bpartners-api/commit/b9dfd5cb3edebfff12c24abd007aa9000db50de6))
* onboarding with Swan ([6711e4b](https://github.com/b-partners/bpartners-api/commit/6711e4bf99c19457f00e96274fbb1b577342238a))
* read and create transaction categories of an account ([ac514a7](https://github.com/b-partners/bpartners-api/commit/ac514a7068a976eacc96c1b95c585b2073262124))
* read known products of an account ([7e6d9a0](https://github.com/b-partners/bpartners-api/commit/7e6d9a05101ec3f3321ae15636d88d3a5b413f2a))
* relaunch invoice manually with custom mail body ([9329801](https://github.com/b-partners/bpartners-api/commit/9329801b7683ddf68a4725708a86a2f8ade3ebd3))
* request payments ([8d56c4d](https://github.com/b-partners/bpartners-api/commit/8d56c4dd0d363459b4d808ea531d71d067e040a7))
* update account holder company info and business activities ([5bb3589](https://github.com/b-partners/bpartners-api/commit/5bb3589489a75197ab3fea81134f883289cea77e))
* update customers for a specific account ([3a88e2e](https://github.com/b-partners/bpartners-api/commit/3a88e2e7fb673f87f468ff91f688e0c1175a51d0))
* verify email on customer creation ([c05c27d](https://github.com/b-partners/bpartners-api/commit/c05c27db06d8035b83258be0f13ce3abfdc508e0))
* whoami ([002f5b8](https://github.com/b-partners/bpartners-api/commit/002f5b8e7b3a226eb8918990a3c6cdc8e26a380a))


### Reverts

* Revert "infra: health check on containerdefinition" ([569479c](https://github.com/b-partners/bpartners-api/commit/569479caaba29cb7b96bb43eb684bfffeb952687))
* Revert "todo(infra): loggroup does not have retention policy on deletion" ([eb12607](https://github.com/b-partners/bpartners-api/commit/eb12607df27cce88e2cb3e5b02199bf408e57727))



