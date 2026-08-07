# [0.87.0](https://github.com/b-partners/bpartners-api/compare/v0.86.1...v0.87.0) (2026-08-07)


### Bug Fixes

* 3d annotation inverted ([354793f](https://github.com/b-partners/bpartners-api/commit/354793fb29fc7a807e188a8d14eebb08b1446050))
* add /users/*/subscriptionCommitments in securityConf ([bb10c44](https://github.com/b-partners/bpartners-api/commit/bb10c4484e602d0d58096d7deac37444082920fa))
* avoid erasing existing subscription data from empty stripe data ([8946244](https://github.com/b-partners/bpartners-api/commit/8946244301453a19bd07fce9c6ef041adf3a92bd))
* blank page after facade section in export pdf ([ad09070](https://github.com/b-partners/bpartners-api/commit/ad0907075f4b12a7f9f61c97f90f7f4f8594c94a))
* **db:** rename commitment_duration _12_MONTHS to TWELVE_MONTHS ([ee0bb7c](https://github.com/b-partners/bpartners-api/commit/ee0bb7c96881a580b8332a1afa1f3c707b45e4b6))
* rename commitment duration enum value 12_MONTHS to TWELVE_MONTHS ([19f2ff8](https://github.com/b-partners/bpartners-api/commit/19f2ff8f29ce8cf1cc9c9c987161ef7f82d3bf01))
* reversed facade image ([645a50d](https://github.com/b-partners/bpartners-api/commit/645a50de94a9e4184f4ac755ed8c02e6ad2325b8))
* **UpdateUserSubscriptionCommitment:** rename autoRenewalStatus into automaticRenewalStatus ([3f2f38d](https://github.com/b-partners/bpartners-api/commit/3f2f38d1687b3233883f16c63bbb9d4d23c9b836))
* **UserSubscriptionCommitmentRestMapper:** verify subscription plan existence ([9d3f7ed](https://github.com/b-partners/bpartners-api/commit/9d3f7ed2fbe411b00818394b311afcadfc0151a7))


### Features

* retrieve user subscription commitments ([655aadd](https://github.com/b-partners/bpartners-api/commit/655aadd5158873ee57b09a3ff560be407f5f9896))
* save user subscription commitments ([4e80959](https://github.com/b-partners/bpartners-api/commit/4e8095953fab1e0086b0b6d848e1fe23e1eb3cfc))
* update user subscription commitment auto renewal status ([19e1598](https://github.com/b-partners/bpartners-api/commit/19e159831117923032747b1b79273d9d10653532))


### Reverts

* pdf optimization prod ([a91b3ee](https://github.com/b-partners/bpartners-api/commit/a91b3ee1ac45bd2cd6fb6709e8c0e10816375a74))



## [0.86.1](https://github.com/b-partners/bpartners-api/compare/v0.86.0...v0.86.1) (2026-08-05)


### Bug Fixes

* **SubscriptionPlan:** add deprecated attribute ([7b52fe2](https://github.com/b-partners/bpartners-api/commit/7b52fe2cb73566e04ac819a514f21798053fdcc5))
* **SubscriptionPlan:** add displayPosition attribute ([f8ad37f](https://github.com/b-partners/bpartners-api/commit/f8ad37fca8c34d33b6d695f58e440803c2e6e407))



# [0.86.0](https://github.com/b-partners/bpartners-api/compare/v0.85.0...v0.86.0) (2026-08-05)


### Bug Fixes

* add user subscription plan ([473c2be](https://github.com/b-partners/bpartners-api/commit/473c2be821dbbb18b3495d590006bb5ce6bcc059))
* associate user into subscription product through stripe webhook ([0f4d3ee](https://github.com/b-partners/bpartners-api/commit/0f4d3ee679cd34e540f5efb9a5db47feb05573ae))
* **ConsumptionFreeTrialValidator:** allow non trial period user to consume over limited consumption ([d1972fe](https://github.com/b-partners/bpartners-api/commit/d1972fe0c60fff1955b18b45e0984e4be283efb5))
* do not cancel scheduled subscription immediatly to debit payment first before cancelling ([b050f47](https://github.com/b-partners/bpartners-api/commit/b050f477cf843a48ff2f4b0af09ef56474cee191))
* fan out user subscription product backfill to avoid Lambda timeout ([ed9f99d](https://github.com/b-partners/bpartners-api/commit/ed9f99da33661ba52c32b0dbb72d8238c8366a36))
* handle area picture consumption as image_access ([b9b4280](https://github.com/b-partners/bpartners-api/commit/b9b428081c09eeb8d23a06f427b00c05f9520c3e))
* handle CANCELED subscription status with correct mapping ([335fea9](https://github.com/b-partners/bpartners-api/commit/335fea964d4f3a38c785eab25170b781785732b0))
* handle subscription plan dynamically in StripeWebhook ([c2ca0c6](https://github.com/b-partners/bpartners-api/commit/c2ca0c60802988d4b0a0e01fbf730ce390887b4c))
* ignore stripe susbcription with CANCEL_AFTER_FIRST_INVOICE_METADATA_KEY flag during initiation ([b4b30a9](https://github.com/b-partners/bpartners-api/commit/b4b30a94879a66f516d787c4ee0023a894153c47))
* limit upcoming billed users by user with subscription ID only ([a1cf899](https://github.com/b-partners/bpartners-api/commit/a1cf899c30e6b3149b7f00b5fa27382517d46e7e))
* map createdAt in toCrupdatedAreaPictureDetails mapper ([6216197](https://github.com/b-partners/bpartners-api/commit/6216197732b9b24fa0c0540875aaebb5f9d8698b))
* oriented pan on pdf export ([de875a2](https://github.com/b-partners/bpartners-api/commit/de875a2686a939df60b3ce480507016a1c062912))
* remove excluded user skipping free consumption validator ([4afdd87](https://github.com/b-partners/bpartners-api/commit/4afdd8782c5e9a69deb98d9d3b6e44209526bbc6))
* separate price with vat and without on subscription plan ([46b6f64](https://github.com/b-partners/bpartners-api/commit/46b6f646b288dcedbc1ddc9077a3672d52e601f3))
* set scheduler to run on 1st of each month to generate past month invoices ([fddddd8](https://github.com/b-partners/bpartners-api/commit/fddddd8f34094289738f38c210716d1732690e68))
* **StripePortalService:** verify stripe customer association before initiating billing portal session ([ab027ee](https://github.com/b-partners/bpartners-api/commit/ab027ee1ec8a6d0f8c29f22b7ca7e0b7ac3bfcbc))
* **StripeWebhookService:** handle subscription_schedule.created event ([eda31d2](https://github.com/b-partners/bpartners-api/commit/eda31d203d05ae73995d4ffa9839ca9241d0b6ef))
* **SubscriptionPlan:** add most chosen attribute ([553957c](https://github.com/b-partners/bpartners-api/commit/553957c678227531174804b4063bcc7bd7da1d01))
* **SubscriptionService:** avoid duplicated overall consumption debit through SET against default INCREMENT ([052ea48](https://github.com/b-partners/bpartners-api/commit/052ea48ab2c9f3df3fa2140ccb63f02633a5a807))
* **SubscriptionService:** cancel latest subscription support scheduled subscriptions ([9ada8b6](https://github.com/b-partners/bpartners-api/commit/9ada8b6f7397a4e556f63ef78ab36ade4eb8cba4))
* trailing page on exported pdf ([a8f1dc1](https://github.com/b-partners/bpartners-api/commit/a8f1dc1e1aee0d2e5ce5b70934671b5cbfee1aa2))
* trigger user subscription product back fill ([db2d776](https://github.com/b-partners/bpartners-api/commit/db2d776a7c5a6dc5d7f42a38712800303122a017))
* **UserRestMapper:** return plan on V1 rest mapper ([9f815c0](https://github.com/b-partners/bpartners-api/commit/9f815c0bc57b4fcd2d6040e86c49142a610e4293))


### Features

* add comment on prospect creation and update ([4a3c865](https://github.com/b-partners/bpartners-api/commit/4a3c86535f980dbfb15a5ad26e5aae33de2c4ab1))
* add new subscription plans ([414c5f9](https://github.com/b-partners/bpartners-api/commit/414c5f93f53f900cc7829f794b94f46b5c3423a0))
* handle subscription plans with actual unique plan dynamically ([683e814](https://github.com/b-partners/bpartners-api/commit/683e8140e78b084407eccbc00bfe23ba34d636a4))
* implement and test geodata imagery for area pictures ([e297f0b](https://github.com/b-partners/bpartners-api/commit/e297f0b76d02c6ba5750890913172b4477bd9665))


### Reverts

* "chore: optimize PDF export performance and file size" ([c3eafeb](https://github.com/b-partners/bpartners-api/commit/c3eafebe00fba0eccf2693f7fa82f3ca448e16c5))



# [0.85.0](https://github.com/b-partners/bpartners-api/compare/v0.84.0...v0.85.0) (2026-07-21)


### Features

* use translated polygon on pdf export if available ([2be0716](https://github.com/b-partners/bpartners-api/commit/2be0716d65d2686031ee61c345709fb8ff3f94d0))



# [0.84.0](https://github.com/b-partners/bpartners-api/compare/v0.83.0...v0.84.0) (2026-07-21)


### Bug Fixes

* **UserSubscription:** compute year month using zone ID ([ec902b1](https://github.com/b-partners/bpartners-api/commit/ec902b1ef5487f9d6b73c4cb1ebb2c17dffd864d))


### Features

* get user subscription invoices ([a5da20e](https://github.com/b-partners/bpartners-api/commit/a5da20e7cf3f23971f4d08b7ac41388f5ca0709e))



# [0.83.0](https://github.com/b-partners/bpartners-api/compare/v0.82.0...v0.83.0) (2026-07-17)


### Bug Fixes

* **SecurityConf:** allow authenticated users to request invoice export not only ADMIN ([460053f](https://github.com/b-partners/bpartners-api/commit/460053f2fb8a9ae4ddfa8e83825fc76bc086da1a))
* **User:** keep identification=VALID_IDENTITY for retro-compatibility ([90d1cee](https://github.com/b-partners/bpartners-api/commit/90d1cee44edfebf3dac3646021bd186e2619dd78))
* **User:** keep idVerified=true for retro-compatibility ([7a63f6b](https://github.com/b-partners/bpartners-api/commit/7a63f6bb8196edc7eb9ae5a3f0a10b9b1c1f6cf8))


### Features

* handle invoice export asynchronously ([d9e54e3](https://github.com/b-partners/bpartners-api/commit/d9e54e3c032f9247a2c9413b2ce105bb5761de3a))



# [0.82.0](https://github.com/b-partners/bpartners-api/compare/v0.81.0...v0.82.0) (2026-07-15)


### Bug Fixes

* download image from current layer on first iteration ([ddbf444](https://github.com/b-partners/bpartners-api/commit/ddbf444983ae9a115d6b535a6320971021d76aba))
* **WmsImageSourceFacade:** iterate over all available layers ([874dc44](https://github.com/b-partners/bpartners-api/commit/874dc44d46cb8be6776d308b0bdf2f0c2eb29b2d))


### Features

* add facade measurements to pdf ([75c961a](https://github.com/b-partners/bpartners-api/commit/75c961aec63d73e897d08b70966b5fa30a3ad66a))



# [0.81.0](https://github.com/b-partners/bpartners-api/compare/v0.80.0...v0.81.0) (2026-07-09)


### Bug Fixes

* **CustomerExportFunction:** export row only for non null CustomerExport payload ([79fe3f6](https://github.com/b-partners/bpartners-api/commit/79fe3f63681b4f54ce5515f112423819bd1a6820))
* **export-pdf:** use user address in user info ([1c516d7](https://github.com/b-partners/bpartners-api/commit/1c516d712b7b7e3d00a0dba94c357ecbb1da7c2f))
* implement GET /users for ADMIN role with V2User ([e84ec3f](https://github.com/b-partners/bpartners-api/commit/e84ec3f302a203c5fe1ad73c5995c81c321d5f74))
* **MonthlySubscriptionInvoiceRequestedService:** avoid duplication on retryer through title and user debited id ([51e57fa](https://github.com/b-partners/bpartners-api/commit/51e57faabac13bfbfb51835162259f4a81fac12c))
* **MonthlySubscriptionInvoiceRequestedService:** configure invoice date period to actual month ([b5653e9](https://github.com/b-partners/bpartners-api/commit/b5653e93c3bc873b9ebfbcf64bd9bc10d56655fd))
* **MonthlySubscriptionInvoiceRequestedService:** verify upcoming invoice is before next month not actual month ([59f2a24](https://github.com/b-partners/bpartners-api/commit/59f2a2447b320640f196319e3d178476a5e8affa))
* **MonthlySubscriptionInvoiceTriggeredService:** export upcoming debited customer for actual month not next ([3d509a1](https://github.com/b-partners/bpartners-api/commit/3d509a1feead424e4ad53b398fe486326874096a))
* **OnboardingService:** use spring proxy to apply transactional commit on each user onboarding ([e804ec8](https://github.com/b-partners/bpartners-api/commit/e804ec852d5142fbad340c780f445cc8b2c69c2e))
* **RefreshInvoiceSummaryTriggeredService:** isolate each user invoice summary refresh event ([39062d1](https://github.com/b-partners/bpartners-api/commit/39062d15ac98ab8a984eddf3639728faac780ae0))
* retrieve paymentMethod during GET /users ([ad2cba3](https://github.com/b-partners/bpartners-api/commit/ad2cba33bc2d0265eeb64837d8b8bea5a80ca654))
* **UserOnboardedService:** verify if user not already linked to stripe customer before (re)processing ([14e9dc8](https://github.com/b-partners/bpartners-api/commit/14e9dc8979b298fe42bda97cd89913fdf2cc98ae))
* **UserRepositoryImpl:** do not retrieve payment method from stripe on list retrieving ([ea347ef](https://github.com/b-partners/bpartners-api/commit/ea347efff852562b755997a267e6d3b0bc87b88a))
* **UserRepository:** pagination offest computed using both page and size not page only ([971be80](https://github.com/b-partners/bpartners-api/commit/971be805bc32f0fa483ae9604ca7832f10910c45))
* **UserRestMapper:** avoid NPE for provided null domain ([aafb184](https://github.com/b-partners/bpartners-api/commit/aafb1845ce3e517f6444e274961c69471d7c1280))


### Features

* **export-pdf:** customizable pages ([11277e0](https://github.com/b-partners/bpartners-api/commit/11277e0284e2dab2c4ce3b9b177c80ba27ad5f75))
* optional export annotation content ([ede0cfa](https://github.com/b-partners/bpartners-api/commit/ede0cfa362bb27ecfd1e7818649d429cdd2f3c9c))
* POST /monthlyUpcomingDebitedCustomers/{year}/{month} for ADMIN_ROLE ([ba49f60](https://github.com/b-partners/bpartners-api/commit/ba49f6039b00738486cbe51a229f61bd89762c66))
* update invoice statuses ([15953e0](https://github.com/b-partners/bpartners-api/commit/15953e07522d476a89de22f07ec6403fa4b02136))



# [0.80.0](https://github.com/b-partners/bpartners-api/compare/v0.79.0...v0.80.0) (2026-06-04)


### Features

* pan edge label in export pdf ([8c84bb7](https://github.com/b-partners/bpartners-api/commit/8c84bb7d99c7ded698d5d16731af9004e3c89d37))



# [0.79.0](https://github.com/b-partners/bpartners-api/compare/v0.78.1...v0.79.0) (2026-05-27)


### Bug Fixes

* add logo compression consumers as beans ([c15a3c7](https://github.com/b-partners/bpartners-api/commit/c15a3c749ac89e803829ed8eb6a3c1d61584575b))
* add shift_direction attribute in HAreaPicture ([6a52a36](https://github.com/b-partners/bpartners-api/commit/6a52a36596d869ceb150d46dc7fc8f6c095b3f22))
* allow zoom level under BUILDING on 20cm image precision ([9cd2d23](https://github.com/b-partners/bpartners-api/commit/9cd2d23b0482f3d094eca3a790a5aa8887bec309))
* always verify user payment method even during trial period ([35683a2](https://github.com/b-partners/bpartners-api/commit/35683a2a76791ee937755f58118b77d3d3e50b2c))
* **CustomerExportHistorySavedService:** rename attachment to correct xlsx extension ([0f8b6a4](https://github.com/b-partners/bpartners-api/commit/0f8b6a444f02f8c8975a21632f8affd727055906))
* **CustomerExportPayload:** export unique customer using stripeCustomerIdentifier attribute ([175e516](https://github.com/b-partners/bpartners-api/commit/175e516da6d02eaf0ab30294bb7256c18ec11f54))
* explicit user white listed scope and remove redondant verification through api full authorization ([2268785](https://github.com/b-partners/bpartners-api/commit/22687858c31b677801d3d46f57b2a87001724ab1))
* global rate not shown when llm null on pdf export ([65a66d2](https://github.com/b-partners/bpartners-api/commit/65a66d27ca53fceab01b55fe8bf6833d34f92920))
* **InternalToRestExceptionHandler:** handle AuthenticationException to 403 http response ([e613c03](https://github.com/b-partners/bpartners-api/commit/e613c033b5554594b82c71589a2c7df525f02191))
* **InvoiceExportLinkRequestedService:** add customer name and year month FR translated on zipped invoices ([28d2d93](https://github.com/b-partners/bpartners-api/commit/28d2d934137029f0eafab8fa669b6c770aae7794))
* **InvoiceExportLinkRequestedService:** only send mail to admin and avoid duplicated empty mail ([e4ba873](https://github.com/b-partners/bpartners-api/commit/e4ba873e717f139f268a214dc190ed47c6fd478f))
* **MonthlySubscriptionInvoiceRequestedService:** compute invoice when next invoice date before sixth of next month ([df17777](https://github.com/b-partners/bpartners-api/commit/df177774ce11cf4b42730ae62d874c2aa995bf1a))
* **MonthlySubscriptionInvoiceRequestedService:** set invoice details to actual month period not last month ([50b6e4c](https://github.com/b-partners/bpartners-api/commit/50b6e4c15df01730bcf24d5a133ca005dac6a509))
* page content indexing in export pdf ([70945f5](https://github.com/b-partners/bpartners-api/commit/70945f504a4e7e1d1247adfcb11d1f9f39d274dc))
* redirect to dashboard page instead of stripe setup workflow when scheduled subscription case ([2e89090](https://github.com/b-partners/bpartners-api/commit/2e890905bf85d9865f058e8ced5bd3665abf4b5d))
* return real user api keys not authenticated user keys ([d6dde13](https://github.com/b-partners/bpartners-api/commit/d6dde131add96bca636dd803fc89e3f8eeafd790))
* **StripeFactory:** ask for payment method during subscription only if any already associated ([791f146](https://github.com/b-partners/bpartners-api/commit/791f14666897b4a963c22136606b59b1501671a2))
* **StripeFactory:** redirect to dashboard url not api url after subscription schedule ([cd00b1a](https://github.com/b-partners/bpartners-api/commit/cd00b1a051205ad99dba52c9533ba289ff0aed85))
* **SubscriptionService:** return existing susbcriptions even if free tria period active when stripe susbcriptions not empty ([a9615cd](https://github.com/b-partners/bpartners-api/commit/a9615cd2c18d6ee6ff9529bccf45390d43a9fc8f))
* **SubscriptionService:** set default active subscription period end to fifth of next month minus 1s ([6205146](https://github.com/b-partners/bpartners-api/commit/620514674a74cc636ff76697decc49701964535b))
* **SubscriptionService:** use today on subscription initiation when endOfTrial period before today ([1184492](https://github.com/b-partners/bpartners-api/commit/11844922ea7de3cf5ae7bd388a4aa3fb62728d87))
* trigger email notification on onboarded user after analysis api keys generated ([9a12a23](https://github.com/b-partners/bpartners-api/commit/9a12a236ebf94598d04d08facca0447c0e48c618))
* **UpcomingDebitedCustomerExport:** ajust exported customers data including extra stripe informations ([c71b812](https://github.com/b-partners/bpartners-api/commit/c71b8120d4d61348ce5dbffe709c669783ae395c))
* **UpcomingDebitedCustomerExportRequested:** add default no-args constructor ([fed0c9b](https://github.com/b-partners/bpartners-api/commit/fed0c9b76889abe76aba9a990addd19aa1a45a06))
* **UpcomingUserDebitService:** avoid NPE when customer address not provided ([785627b](https://github.com/b-partners/bpartners-api/commit/785627bd940be346f4c7fe1c1ef833d98c3deeb6))
* use areaPicture.geoPositions instead of tile in getAvailableLayersFrom ([826f9a9](https://github.com/b-partners/bpartners-api/commit/826f9a99a270c99f458c15dd88f3f0bc56ca45fc))
* **UserAnalysisApiKeyRequested:** update actual user.apiKey to analysisApiKey ([c5a1a32](https://github.com/b-partners/bpartners-api/commit/c5a1a323ca59f8d76237923c009443e0cdd3e2b1))
* **UserCustomerConverter:** associate converted customer from user through default user to credit identifier ([c185f7b](https://github.com/b-partners/bpartners-api/commit/c185f7b42fdf423309b3cc83d661978978bb828a))
* **UserOnboarded:** persist generated api key through subscriptionService to avoid bad sql transactions handling ([eeb3521](https://github.com/b-partners/bpartners-api/commit/eeb3521e4c34e67889ce7721335be4e2c136fdb2))
* **UserRestMapper:** also return ACTIVE subscription status even if trial period not expired but user subscribe on stripe ([740d03b](https://github.com/b-partners/bpartners-api/commit/740d03b1445cc12547f62acec5b80d5af38dcd85))
* **UserSubscription:** always require payment method even if user not in trial period ([5f2530b](https://github.com/b-partners/bpartners-api/commit/5f2530b9a566221d7ebcab5028c077bd8ee6b1a2))
* **UserSubscription:** compute default period when SUBSCRIPTION_VALIDATION_NOT_REQUIRED for user white listed ([20aafd8](https://github.com/b-partners/bpartners-api/commit/20aafd8a9121b9411afd6ce86a2df067b989c3f1))
* **UserSubscription:** map payment method through user whitelisted ([648406d](https://github.com/b-partners/bpartners-api/commit/648406db29f9360a7fad41ce28d65bcb9a8bc9e9))
* **UserSubscription:** return CANCELLED status only when latest subscription ends actual or next month ([8e6204f](https://github.com/b-partners/bpartners-api/commit/8e6204f7faa460fa089215ce54b81bcaab796fa1))
* **UserSubscriptionSession:** save subscription session creation datetime ([aa25318](https://github.com/b-partners/bpartners-api/commit/aa2531803f7a8fb9720ab36c71d61c9039507956))


### Features

* 3d pans in export pdf ([b319a12](https://github.com/b-partners/bpartners-api/commit/b319a12269c51a60404d018034bc52e4a409ead1))
* add delete prospect by id endpoint ([8c008df](https://github.com/b-partners/bpartners-api/commit/8c008df211d32d0eb3f7f5bf4f021c569d4c92aa))
* add luxembourg area ([4971fdf](https://github.com/b-partners/bpartners-api/commit/4971fdf49f8c52edabedc452c417bee13d3b5607))
* initiate payment methods insertion using setup ([cead89c](https://github.com/b-partners/bpartners-api/commit/cead89c533a0ee008ab621cf63548266d82d4dc8))
* retrieve invoice export request by its id ([76f0a61](https://github.com/b-partners/bpartners-api/commit/76f0a612c65bc3f2265ae40bebce8bdc5a27757a))
* support switzerland area ([648a070](https://github.com/b-partners/bpartners-api/commit/648a070244de57743e412dac0e54781e78f58f92))


### Reverts

* **Drawer:** from 35cd87b6628921741dd144eeb23ddd94b4e73245 into a627df37f635edf843d646a65f53335a5acb79a3 ([#1680](https://github.com/b-partners/bpartners-api/issues/1680)) ([6ce8de8](https://github.com/b-partners/bpartners-api/commit/6ce8de8db548b85cc2db7147facccec8268e9a7e))



