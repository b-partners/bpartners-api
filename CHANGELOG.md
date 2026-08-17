# [0.88.0](https://github.com/b-partners/bpartners-api/compare/v0.87.0...v0.88.0) (2026-08-17)


### Bug Fixes

* ajust billing cycle anchor per calendar month and prorated ([96d29a2](https://github.com/b-partners/bpartners-api/commit/96d29a29d6a457c73f1a6ac1f475652f20700fac))
* debit immediately when annual subscription billing interval ([e4bcbd1](https://github.com/b-partners/bpartners-api/commit/e4bcbd1cc34e6afa75e51a94a354cfcd9692dcc5))
* x axis flip of 3d annotation in pdf export ([e3644f4](https://github.com/b-partners/bpartners-api/commit/e3644f4f93fed47e5c341fef8634cee8139be107))


### Features

* handle annual pricing with new subscription plans ([392d770](https://github.com/b-partners/bpartners-api/commit/392d770a8d89be4530900882a309c9f059d38a4a))



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



