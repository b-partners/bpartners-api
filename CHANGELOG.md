# [0.91.0](https://github.com/b-partners/bpartners-api/compare/v0.85.0...v0.91.0) (2026-09-03)


### Bug Fixes

* 3d annotation inverted ([354793f](https://github.com/b-partners/bpartners-api/commit/354793fb29fc7a807e188a8d14eebb08b1446050))
* add /users/*/subscriptionCommitments in securityConf ([1ee050b](https://github.com/b-partners/bpartners-api/commit/1ee050b8aca9de315c6bb61b7164e6a6811383f7))
* add billing interval on user subscription ([9788f97](https://github.com/b-partners/bpartners-api/commit/9788f975aaa50a31f208c8f50a995bafa6b0d6ee))
* add detection identifier on tracking to avoid duplication ([0918081](https://github.com/b-partners/bpartners-api/commit/09180812a5018bbfb580e1cdea936919a987e65b))
* add user subscription plan ([ab43b4b](https://github.com/b-partners/bpartners-api/commit/ab43b4b2b0d879af9d3e76016357b97691e64e85))
* ajust billing cycle anchor per calendar month and prorated ([9be6de6](https://github.com/b-partners/bpartners-api/commit/9be6de668bf5c4a3abf671b4fb4aef9c81d06a3a))
* annotation draft filter compute timeout ([76df2bc](https://github.com/b-partners/bpartners-api/commit/76df2bc454b2e6aa790ddd5f34c244514640de70))
* associate user into subscription product through stripe webhook ([36b9c97](https://github.com/b-partners/bpartners-api/commit/36b9c97d6f1b1efe7b0265c08896301e56853886))
* avoid erasing existing subscription data from empty stripe data ([576f753](https://github.com/b-partners/bpartners-api/commit/576f75322167d1d7d864cd0b68f36e811efc433f))
* blank page after facade section in export pdf ([ad09070](https://github.com/b-partners/bpartners-api/commit/ad0907075f4b12a7f9f61c97f90f7f4f8594c94a))
* cap concurrent GeoData Imagery API calls in draft-annotations batching ([ca7e548](https://github.com/b-partners/bpartners-api/commit/ca7e548f01193771f1780946d3af527315c5d2d3))
* **ConsumptionFreeTrialValidator:** allow non trial period user to consume over limited consumption ([4d45d35](https://github.com/b-partners/bpartners-api/commit/4d45d35a17d856fe1bc4e315a2a5fc781756b0a2))
* **db:** rename commitment_duration _12_MONTHS to TWELVE_MONTHS ([63e5450](https://github.com/b-partners/bpartners-api/commit/63e5450e2655ec5b78a19e96074018043b94d413))
* debit immediately when annual subscription billing interval ([91002c0](https://github.com/b-partners/bpartners-api/commit/91002c06fcbcf3aeb36286f35083d6902d43d133))
* do not cancel scheduled subscription immediatly to debit payment first before cancelling ([62e4d78](https://github.com/b-partners/bpartners-api/commit/62e4d788e5385581bac6389b9ecf55692e6df3d5))
* do not include and schedule overage subscription anymore ([c335473](https://github.com/b-partners/bpartners-api/commit/c3354737e09795dc8aa8551a0699a66ca61bba08))
* do not update stripe subscriptions in a terminal status ([e57d8bf](https://github.com/b-partners/bpartners-api/commit/e57d8bf41a7fc91f4afce374011eb4b377e68849))
* eliminate N+1 queries on draft area picture annotations endpoint ([99728e9](https://github.com/b-partners/bpartners-api/commit/99728e954aeafbe911f93a6f5e5f1390889e902a))
* fan out user subscription product backfill to avoid Lambda timeout ([6909d83](https://github.com/b-partners/bpartners-api/commit/6909d83254e5f827d461a19ce99238416331efb5))
* **flyway:** set lock timeout on migration connections ([e52d290](https://github.com/b-partners/bpartners-api/commit/e52d290215a9c2bad8a7ab40d49abaf3ef29d298))
* grant credits from subscription and renew each month through scheduler ([5150619](https://github.com/b-partners/bpartners-api/commit/5150619dd80521f5d59a1223d7623d3c23a3c019))
* grant subscription credits on plan change and until period end ([09574a5](https://github.com/b-partners/bpartners-api/commit/09574a553142596c2fc2bec9fbd46d69afe44cfe))
* handle already submitted credit purcahse on invoice generation ([187a583](https://github.com/b-partners/bpartners-api/commit/187a5837b80c1ce591d2352365d84cc1468112f3))
* handle analysis consumption on stripe event webhook on YEARLY billing interval ([4fcc39d](https://github.com/b-partners/bpartners-api/commit/4fcc39d043e1725d7aa7deed283ea50617250abf))
* handle area picture consumption as image_access ([b9b4280](https://github.com/b-partners/bpartners-api/commit/b9b428081c09eeb8d23a06f427b00c05f9520c3e))
* handle CANCELED subscription status with correct mapping ([3f10d30](https://github.com/b-partners/bpartners-api/commit/3f10d30a1c83828a5c8e0413aa4aceff2f620a0e))
* handle subscription plan dynamically in StripeWebhook ([90a79c9](https://github.com/b-partners/bpartners-api/commit/90a79c98b3dcbe50b72799a11cd758eaac5b65ba))
* ignore png compression if failed ([4ad10ce](https://github.com/b-partners/bpartners-api/commit/4ad10cec0c4ce54780e19a9d496ded48ba1f89f1))
* ignore stripe susbcription with CANCEL_AFTER_FIRST_INVOICE_METADATA_KEY flag during initiation ([f74049a](https://github.com/b-partners/bpartners-api/commit/f74049a1cf517e65318659f286db588cc51f1e3a))
* **InvoiceValidator:** allow crupdating PAID invoice ([4b309b1](https://github.com/b-partners/bpartners-api/commit/4b309b13a8286a5c0dc322f7645e969cff52f747))
* limit upcoming billed users by user with subscription ID only ([b343055](https://github.com/b-partners/bpartners-api/commit/b343055b24e83571a43b1f89f3fd884231f278d4))
* make file info nullable for downloadImage AreaPicture ([6bcbb35](https://github.com/b-partners/bpartners-api/commit/6bcbb35acd58908c22935e8c573a79cb23124f34))
* map createdAt in toCrupdatedAreaPictureDetails mapper ([6216197](https://github.com/b-partners/bpartners-api/commit/6216197732b9b24fa0c0540875aaebb5f9d8698b))
* npe on AreaPictureAnnotation mapper while no ProspectId ([61d22b6](https://github.com/b-partners/bpartners-api/commit/61d22b62554669be70507de33194c538e76b7a36))
* oriented pan on pdf export ([de875a2](https://github.com/b-partners/bpartners-api/commit/de875a2686a939df60b3ce480507016a1c062912))
* preserve sub-unit precision in polygon coordinates until pixel rounding ([4788e4b](https://github.com/b-partners/bpartners-api/commit/4788e4b6c7342d4586276f59664f3bfd00ace3ae))
* price credits at public price once subscription is cancelled ([02dbbb7](https://github.com/b-partners/bpartners-api/commit/02dbbb70521afe253c049a6280fefa73bafb2dcf))
* remove excluded user skipping free consumption validator ([4afdd87](https://github.com/b-partners/bpartners-api/commit/4afdd8782c5e9a69deb98d9d3b6e44209526bbc6))
* rename commitment duration enum value 12_MONTHS to TWELVE_MONTHS ([1c6507e](https://github.com/b-partners/bpartners-api/commit/1c6507e355e2a35fc3d33b333572f37cb80f3103))
* reversed facade image ([645a50d](https://github.com/b-partners/bpartners-api/commit/645a50de94a9e4184f4ac755ed8c02e6ad2325b8))
* send subscription invoice to a single recipient ([701d13b](https://github.com/b-partners/bpartners-api/commit/701d13b58b3247cade045cd3e8fd2a79b4b67ea9))
* separate price with vat and without on subscription plan ([a490d51](https://github.com/b-partners/bpartners-api/commit/a490d5138f80a9d5332b1bfe414e37b32f9edd37))
* set scheduler to run on 1st of each month to generate past month invoices ([fddddd8](https://github.com/b-partners/bpartners-api/commit/fddddd8f34094289738f38c210716d1732690e68))
* set subscription cancellation configurable with immediate effect by default ([099c964](https://github.com/b-partners/bpartners-api/commit/099c9646e474e33a0d91f83932fbeff1afac1aaa))
* **StripeFactory:** allow proration except YEARLY interval ([d352161](https://github.com/b-partners/bpartners-api/commit/d35216131fc42126ad9bf587ad937dfa7e29cdb1))
* **StripePortalService:** verify stripe customer association before initiating billing portal session ([12a20c3](https://github.com/b-partners/bpartners-api/commit/12a20c3d8a10a60a55659c2bf4c76ed95a5b0474))
* **StripeWebhookService:** handle subscription_schedule.created event ([d31aad6](https://github.com/b-partners/bpartners-api/commit/d31aad6a9cea47787e7c8936b34e7d8ac0b56bd6))
* **Subscription:** compute period from line period not invoice period ([9a93b2b](https://github.com/b-partners/bpartners-api/commit/9a93b2bb134541782409c8a8939c3d5b8220dde7))
* **subscription:** date subscription products on the real period start ([aff8a95](https://github.com/b-partners/bpartners-api/commit/aff8a9559c0b5b1e02cc672a16b0d7cd5d29a5be))
* **SubscriptionInvoice:** improve mail content and prepraid subscription invoice labels ([9ffa752](https://github.com/b-partners/bpartners-api/commit/9ffa7526aa31a8643bdf213d64044aa83d2c6c76))
* **subscription:** keep the plan served until the subscription end ([0d72636](https://github.com/b-partners/bpartners-api/commit/0d726366ee3f9197d1b2b88943526c179d980ed4))
* **SubscriptionPayment:** derive billing interval from the stripe invoice ([b53711f](https://github.com/b-partners/bpartners-api/commit/b53711fb0edf64f17e0607d0a1e950521ce87943))
* **SubscriptionPlan:** add deprecated attribute ([ba0a5ea](https://github.com/b-partners/bpartners-api/commit/ba0a5eaa56574684452cafd7504fc9178e230a46))
* **SubscriptionPlan:** add displayPosition attribute ([2862886](https://github.com/b-partners/bpartners-api/commit/28628862593b08e2d7302c5c648be6233865c7d9))
* **SubscriptionPlan:** add most chosen attribute ([20606a3](https://github.com/b-partners/bpartners-api/commit/20606a309b5ea5f729fbac44e006a0189cd9520b))
* **SubscriptionProduct:** do not override credit unit price in cents and credit cost per analysis ([27b5c87](https://github.com/b-partners/bpartners-api/commit/27b5c87b76bb592c60b8f419b2458772228be58d))
* **subscription:** reject a subscription when one is already scheduled ([8a2b0fc](https://github.com/b-partners/bpartners-api/commit/8a2b0fc3fe75d8a57deb381276b358aa8b220289))
* **Subscription:** retrieve product from stripe end-to-end id product ([bf95f50](https://github.com/b-partners/bpartners-api/commit/bf95f50965cdbd341d049ead23987b701fcee5b0))
* **subscription:** scope unpaid invoice check to a real subscription during schedule gap ([a4bf693](https://github.com/b-partners/bpartners-api/commit/a4bf693ccfd3c82328d8300378bb362454ebca26))
* **subscription:** scope unpaid stripe invoice check to the current subscription ([8e188d5](https://github.com/b-partners/bpartners-api/commit/8e188d5bac8e7c523b3cfb25e5398c3a2f7fb56d))
* **SubscriptionService:** avoid duplicated overall consumption debit through SET against default INCREMENT ([052ea48](https://github.com/b-partners/bpartners-api/commit/052ea48ab2c9f3df3fa2140ccb63f02633a5a807))
* **SubscriptionService:** cancel latest subscription support scheduled subscriptions ([cd73e0f](https://github.com/b-partners/bpartners-api/commit/cd73e0f2c678a85b925f966cfacbcf91fe5d1c75))
* trailing page on exported pdf ([a8f1dc1](https://github.com/b-partners/bpartners-api/commit/a8f1dc1e1aee0d2e5ce5b70934671b5cbfee1aa2))
* trigger user subscription product back fill ([96e6a40](https://github.com/b-partners/bpartners-api/commit/96e6a40890efbcaaedd6c00c8dabba115c0ccf49))
* **UpdateUserSubscriptionCommitment:** rename autoRenewalStatus into automaticRenewalStatus ([cfe245e](https://github.com/b-partners/bpartners-api/commit/cfe245e43ee4e63f7c495a9bbe34a8eb33d7e107))
* **UserRestMapper:** return plan on V1 rest mapper ([bb18edc](https://github.com/b-partners/bpartners-api/commit/bb18edcaae37f8dc986d306667d2ee0726f6a546))
* **UserSubscriptionCommitmentRestMapper:** verify subscription plan existence ([0d502ed](https://github.com/b-partners/bpartners-api/commit/0d502ed4d772706642524c3c1b334e3c0b91945a))
* x axis flip of 3d annotation in pdf export ([e3644f4](https://github.com/b-partners/bpartners-api/commit/e3644f4f93fed47e5c341fef8634cee8139be107))


### Features

* add comment on prospect creation and update ([4a3c865](https://github.com/b-partners/bpartners-api/commit/4a3c86535f980dbfb15a5ad26e5aae33de2c4ab1))
* add new subscription plans ([91701c1](https://github.com/b-partners/bpartners-api/commit/91701c1b2050649a121d476ae7c2cb49bc7013ea))
* annotation draft filter on get all endpoint ([1c77973](https://github.com/b-partners/bpartners-api/commit/1c779738a76c55c75fb57b959b8ef7220d683727))
* append credit transaction on analysis consumption through detection tracking ([1775cbf](https://github.com/b-partners/bpartners-api/commit/1775cbf6e5de6777abc07a50c2b7770cfc0b9436))
* configure email recipients per account holder ([0e6bac0](https://github.com/b-partners/bpartners-api/commit/0e6bac058c398b18295fef24e61833cbf0a3eca5))
* download image is now optinoal for AreaPicture ([c8a8688](https://github.com/b-partners/bpartners-api/commit/c8a8688c73176a624f511313ccbf2dd003a4fa27))
* filter annotation drafts by properties ([8a39c64](https://github.com/b-partners/bpartners-api/commit/8a39c6457f230505073d1988bec7e78bf3cdc025))
* GET /users/{id}/paymentMethods ([6bbd29c](https://github.com/b-partners/bpartners-api/commit/6bbd29c9cfb82f65289a47ffed532e18ac24f22b))
* handle annual pricing with new subscription plans ([4a0bf76](https://github.com/b-partners/bpartners-api/commit/4a0bf7687a09df906e6848f421165a14d6798fce))
* handle subscription plans with actual unique plan dynamically ([a0c2323](https://github.com/b-partners/bpartners-api/commit/a0c2323c4a7aa268bd69cddcb7c9a16ef017ef82))
* implement and test geodata imagery for area pictures ([e297f0b](https://github.com/b-partners/bpartners-api/commit/e297f0b76d02c6ba5750890913172b4477bd9665))
* notify by email credit purchase invoice ([93b8975](https://github.com/b-partners/bpartners-api/commit/93b89753ccf18dfc3b856b1063e8492148892f7e))
* produces prepraid subscription invoice after user purchase ([dae2229](https://github.com/b-partners/bpartners-api/commit/dae22291303e3a6867960bd917366c7e3edc25f7))
* prospect analyse ([15be829](https://github.com/b-partners/bpartners-api/commit/15be829435ae282c5ec9c061e7c96b0d3aadce69))
* purchase credits with existing packs or custom ([c5520dd](https://github.com/b-partners/bpartners-api/commit/c5520ddfa40efabd5a798ccf602b5a210c86930a))
* replace payment method through PUT /users/{id}/paymentMethods ([18d49b5](https://github.com/b-partners/bpartners-api/commit/18d49b55dbc0e38b1c92b0c53a7d84c348afd597))
* retrieve credit packs ([64d1dc7](https://github.com/b-partners/bpartners-api/commit/64d1dc7880028348d4997a815d7f15c69c9b4f8f))
* retrieve user credit balance ([d2753c4](https://github.com/b-partners/bpartners-api/commit/d2753c46f7e69fbb138d9f2531b9ae335b3aa20c))
* retrieve user credit purchases ([94184d5](https://github.com/b-partners/bpartners-api/commit/94184d5234926b15b73c98c6eead73d8594ba0ed))
* retrieve user credit transactions ([f12d5a3](https://github.com/b-partners/bpartners-api/commit/f12d5a39d751ec93b73e907852052928ecc6aa4d))
* retrieve user detection tracking ([ced310d](https://github.com/b-partners/bpartners-api/commit/ced310d3992d0d031b4f15683d7ab58f5aa97b87))
* retrieve user subscription commitments ([a88475e](https://github.com/b-partners/bpartners-api/commit/a88475ebfe09be317c92819a263bb5ed11f374fc))
* route subscription invoice emails through recipients config ([0448b71](https://github.com/b-partners/bpartners-api/commit/0448b71da68e9b621ac1cf754433c12993c885cb))
* save user subscription commitments ([e7a7ef2](https://github.com/b-partners/bpartners-api/commit/e7a7ef260c5efde922de03f653f86ad06e2b8ede))
* **security:** add public /token/validate endpoint for Cognito token validation ([5d3609b](https://github.com/b-partners/bpartners-api/commit/5d3609b6b48fb93e54452e32b3740fcfd2349599))
* **subscription:** expose renewal status and scheduled next subscription ([5fd5630](https://github.com/b-partners/bpartners-api/commit/5fd5630966954901b3eac1ddf6cd69fe103b00e2))
* **SubscriptionInvoice:** show billing interval in the invoice email ([fbcd6b4](https://github.com/b-partners/bpartners-api/commit/fbcd6b45123b39a1cfa6445f1f24c1851652f2cd))
* support globalImage3DUrl fallback for area picture annotation export ([8032a6e](https://github.com/b-partners/bpartners-api/commit/8032a6e2de04c1f424f31401ca28b55981257284))
* trigger credit invoice after credit purchase completed ([440f767](https://github.com/b-partners/bpartners-api/commit/440f767eaee1521479db5f334745de78c3085d84))
* update user subscription commitment auto renewal status ([92284c0](https://github.com/b-partners/bpartners-api/commit/92284c0bb346c0a6a1432d4ac8208c751e6c6d46))


### Reverts

* "chore: optimize PDF export performance and file size" ([420d7d3](https://github.com/b-partners/bpartners-api/commit/420d7d3980b76ec309ccf028e3a997a25be35c45))
* pdf optimization prod ([a91b3ee](https://github.com/b-partners/bpartners-api/commit/a91b3ee1ac45bd2cd6fb6709e8c0e10816375a74))



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



## [0.78.1](https://github.com/b-partners/bpartners-api/compare/v0.78.0...v0.78.1) (2026-02-03)


### Bug Fixes

* commit untracked files on releases-version ([489aa75](https://github.com/b-partners/bpartners-api/commit/489aa7534da183c9ae0ad544d4e9ffabce878087))



# [0.78.0](https://github.com/b-partners/bpartners-api/compare/v0.77.0...v0.78.0) (2026-02-03)


### Bug Fixes

* do not look for defaut payment only existing payment methods ([aa8c56e](https://github.com/b-partners/bpartners-api/commit/aa8c56e692f86c7c344e31958e4d668852703188))
* handle multiple analysis api key found by one key ([a20aac7](https://github.com/b-partners/bpartners-api/commit/a20aac7d2c53702b3f1b734db8864d9e94ceba26))
* handle one by one user subscription invoice computing ([c2a7488](https://github.com/b-partners/bpartners-api/commit/c2a7488679bc3952453cea6c185c3e58ea896116))
* **MonthlySubscriptionInvoiceRequestedService:** do not compute subscription invoice when already computed for current month ([7c47057](https://github.com/b-partners/bpartners-api/commit/7c47057506ade869d542f1975e75cb209e010571))
* **MonthlySubscriptionInvoiceRequestedService:** filter existing subscritpion invoices using userToCredit and userToDebit filter ([18e1b69](https://github.com/b-partners/bpartners-api/commit/18e1b69e3980c21a857619442ead2e6ab743b6ad))
* **MonthlySubscriptionInvoiceRequestedService:** only compute subscription invoice for active subscription and eligible user ([dd0bfe1](https://github.com/b-partners/bpartners-api/commit/dd0bfe17bce30c20c885d35755a2e54191d62803))
* **MonthlySubscriptionInvoiceRequestedService:** set default sending date to last day of actual month ([20cb8a5](https://github.com/b-partners/bpartners-api/commit/20cb8a5246297c4f5acef0ea7216aacc0ef21cd5))
* **ProspectService:** trigger ProspectUpdate event on each saving methods ([ce8aba1](https://github.com/b-partners/bpartners-api/commit/ce8aba141b6cee4b85cdf4c392c3f56a4b9e7b63))
* **RefreshUserInvoiceSummaryTriggered:** update eventStack=EVENT_STACK_2 ([a0eed3a](https://github.com/b-partners/bpartners-api/commit/a0eed3af1564487b2ae9820406f78c4903840f6a))
* retrieve payment methods from both subscription and customer ([f81d4ee](https://github.com/b-partners/bpartners-api/commit/f81d4ee6b18ea689b3462ba060c2ba4a6116883d))
* **StripeFactory:** avoid billing_cycle_anchor late than natural billing by handling today if before fifth of actual month ([63be005](https://github.com/b-partners/bpartners-api/commit/63be005ab98e592d15e4ca87eb74b0222f71ea6a))
* **SubscriptionService:** update free trial period to 7 days ([ea78623](https://github.com/b-partners/bpartners-api/commit/ea78623fa5289f390b0cfa9d058e724f2133381e))
* use upcoming stripe invoice to compute subscription invoice ([e8b42ac](https://github.com/b-partners/bpartners-api/commit/e8b42ac581a974d1649abd0f04ac339d56bd4eb6))
* **UserController:** type updated user api keys as DASHBOARD and add default creation datetime ([6e415ae](https://github.com/b-partners/bpartners-api/commit/6e415aecc244647c9eb2b7aedd2ebf4e0705ad89))
* **UserOnboarded:** generate api key after user onboarded ([f9f6298](https://github.com/b-partners/bpartners-api/commit/f9f629884798b0eaf639c00f6ddd95aa0216f0f7))
* **UserRestMapper:** always return ACTIVE when not subscription eligibile ([1bc6474](https://github.com/b-partners/bpartners-api/commit/1bc64741706fb145bd277d65e003a06105212351))
* **UserRestMapper:** return ACTIVE only when free trial period not active ([e6940d6](https://github.com/b-partners/bpartners-api/commit/e6940d6f64d9658d17e6023076a34a9dd29530ed))
* **UserSubscription:** only subscription not expired can be valid ([19dba5e](https://github.com/b-partners/bpartners-api/commit/19dba5e7125538acca229b7b386a231ae4a61fe0))


### Features

* implement areaPicture shiftDirection ([67a1ec7](https://github.com/b-partners/bpartners-api/commit/67a1ec74d7a9cb824b20c3f3f35bf51c85b3b0f6))


### Reverts

* Revert "chore(to-revert): update invoice trigger information to december 2025" ([4ed25bf](https://github.com/b-partners/bpartners-api/commit/4ed25bf6c23283ccf4dd193f412dbae8985db5a7))



