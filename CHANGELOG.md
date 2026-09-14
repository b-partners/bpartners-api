# [0.95.0](https://github.com/b-partners/bpartners-api/compare/v0.94.0...v0.95.0) (2026-09-10)


### Bug Fixes

* consume granted credits on analysis during free trial ([2b95b6f](https://github.com/b-partners/bpartners-api/commit/2b95b6f10096800fafe06cd756d3ff398a320481))
* **db:** exclude Module devis automatisé from PRO ([b7ca3ec](https://github.com/b-partners/bpartners-api/commit/b7ca3ecf7699460b80e670de918e6ff920494f60))
* include featureSections and comparisonEntries on stripe product concilliation ([de16bac](https://github.com/b-partners/bpartners-api/commit/de16bac0cb71941b1ea0ee40db343da5023195a6))
* include trial analysis granted on stripe product concilliation ([5160058](https://github.com/b-partners/bpartners-api/commit/5160058f4b3f4f67128f856d6269f517977fbc0f))
* transmit user dashboard api key to analysis api and keep it on analysis key creation ([e9bc436](https://github.com/b-partners/bpartners-api/commit/e9bc4361befbe39812c74763eada733e18ad2098))


### Features

* persist mutation and fireRisk on area picture annotation metadata ([b54f295](https://github.com/b-partners/bpartners-api/commit/b54f295eac5e11756934ff664ae29af76ffb9a33))



# [0.94.0](https://github.com/b-partners/bpartners-api/compare/v0.93.0...v0.94.0) (2026-09-09)


### Bug Fixes

* cap main image size on PDF export title page ([cc43d72](https://github.com/b-partners/bpartners-api/commit/cc43d729d9c6bdd6370b4da3de514756b52a86b7))


### Features

* add per-product free trial with expiring analysis credits, once per user and product ([80d308b](https://github.com/b-partners/bpartners-api/commit/80d308bd457d2798e1c498de5f4a59883f1b4ea9))
* add read-only per-plan free-trial eligibility endpoint ([37b6c83](https://github.com/b-partners/bpartners-api/commit/37b6c835f75ef63d832c1570f5960130249eeb83))



# [0.93.0](https://github.com/b-partners/bpartners-api/compare/v0.92.0...v0.93.0) (2026-09-08)


### Features

* add subscription comparison entries ([1a9d29f](https://github.com/b-partners/bpartners-api/commit/1a9d29fa0e9e282e6efe30a7ef1081a2249456bd))



# [0.92.0](https://github.com/b-partners/bpartners-api/compare/v0.91.0...v0.92.0) (2026-09-08)


### Bug Fixes

* allow resetting email recipients and partial-type PUT ([a878254](https://github.com/b-partners/bpartners-api/commit/a87825435a92a3ad2d092b31df388e5b9ae71c9a))
* **annotation:** format shared roof edge measures with a fixed locale ([2155267](https://github.com/b-partners/bpartners-api/commit/21552670eeb0c1995977c3c4d2c6999b57d90b8a))
* compute roof edge measures once per shared edge ([3e5537f](https://github.com/b-partners/bpartners-api/commit/3e5537f5d0817c6059457e4cd6a61d26e041b515))
* consider existing spendable credits as valid subscription ([03638dd](https://github.com/b-partners/bpartners-api/commit/03638dd4d0e68d4791d03ba91e563570be87fc8b))
* **credit:** grant subscription credits on paid invoice and cap transitional credits to the migration day ([be54a38](https://github.com/b-partners/bpartners-api/commit/be54a38f0242f5a7db1aca76abf82f0c4bd5e59d))
* **credit:** prevent expired-lot consumption from reducing later grants ([948451a](https://github.com/b-partners/bpartners-api/commit/948451acff09225c5c7f82386cde17d0f18cefc9))
* **DetectionTrackingJpaRepository:** explicit string cast on each criteria ([94c434c](https://github.com/b-partners/bpartners-api/commit/94c434c5576d42bb79071fec42787bfcf6a809c8))
* **EmailRecipientsValidator:** allow empty recipient mail ([f7ee32c](https://github.com/b-partners/bpartners-api/commit/f7ee32c0083274d0626320885bbf5c528ea8b144))
* exempt subscription validation for onboarding requests ([c7d3cd9](https://github.com/b-partners/bpartners-api/commit/c7d3cd90e704db151184493bcfe2976d6fa376fd))
* **mail:** avoid NPE on null unit price and quantity of credit purchase invoice line ([383752e](https://github.com/b-partners/bpartners-api/commit/383752e00c8986c643bb3a754a94a15add8aa9f2))
* **mail:** read the credit unit price from the purchase instead of the invoice line ([7923650](https://github.com/b-partners/bpartners-api/commit/7923650faf59218703c59385fb3ec1674d7daed8))
* resolve invoice recipient holder from customer not issuer ([6a9b468](https://github.com/b-partners/bpartners-api/commit/6a9b46819955abfbc1c38c94f5082fdc664f713c))
* route credit purchase mail to specific recipient same as subscription invoice ([f48400c](https://github.com/b-partners/bpartners-api/commit/f48400cb7def73272d20a8ff880500981b20df8a))
* **security:** exempt subscription validation for creditPurchases GET and PUT ([cbf6410](https://github.com/b-partners/bpartners-api/commit/cbf641015a46488113740acae4c425310b4ca0da))
* **StripeWebhookService:** cancel only deprecated ESSENTIAL product after payment ([9d76ac7](https://github.com/b-partners/bpartners-api/commit/9d76ac79feb0071d79df3d659fa04774fbed601d))
* **SubscriptionProduct:** include feature from other subscription product ([91fe333](https://github.com/b-partners/bpartners-api/commit/91fe333d81abceeec27aa8ad09b2fdbe6f0c9cb3))
* **subscription:** release managing schedule before end-of-period cancellation ([fb52e97](https://github.com/b-partners/bpartners-api/commit/fb52e970f60515049772e4e179ad1a856b2a4089))
* **subscription:** report Stripe's exclusive period end as the last served day on invoices ([da0cc06](https://github.com/b-partners/bpartners-api/commit/da0cc06be86a1448dd2026e9013d3e55bebeba9e))
* **subscription:** report the served period of terminated subscriptions ([5e4e257](https://github.com/b-partners/bpartners-api/commit/5e4e257066a2f3a39ab85bdab73c74858b7270d3))
* **test:** mock StripeDefaultPaymentMethodService in IT contexts without a StripeClient bean ([b3a3b7c](https://github.com/b-partners/bpartners-api/commit/b3a3b7cd3869c90ed056b0cd1475e9d872258be5))
* **UsernamePasswordAuthenticatorFacade:** include PUT /users/*/paymentMethods exempting subscription validation ([06a001b](https://github.com/b-partners/bpartners-api/commit/06a001b7aa8e3524a64892ac58c38795289a6ea7))


### Features

* add lite variant for draft area picture annotations list endpoints ([6b4b352](https://github.com/b-partners/bpartners-api/commit/6b4b352405495690c4efaf877dd8844a7b7fca1c))
* **credit:** grant transitional credits to active stripe subscribers ([d01e062](https://github.com/b-partners/bpartners-api/commit/d01e06238a9ff44ffbbd961d0b1dee5fbc1bef32))
* **invoice:** bill a pack credit purchase as one line per pack ([19c9e5f](https://github.com/b-partners/bpartners-api/commit/19c9e5fa73d48cb2ae94ae7d7ae7803ccde46975))
* **mail:** show credit count without redundancy and unit price excluding VAT in credit purchase invoice mail ([c676580](https://github.com/b-partners/bpartners-api/commit/c67658031f0bf68de96f1ccf20834941d782c212))
* **subscription:** cancel subscriptions immediately on paid invoice and via admin trigger ([693ec98](https://github.com/b-partners/bpartners-api/commit/693ec984bccf50afbce4c5d2088018c40ad0795c))
* **subscription:** expose structured feature sections and inherited plan for exact plan rendering ([9a27701](https://github.com/b-partners/bpartners-api/commit/9a27701b016f15f7bbfc044a59475b8dc7da7c69))
* **subscription:** gate credits at analysis time instead of at authentication ([6038ad1](https://github.com/b-partners/bpartners-api/commit/6038ad1cac135ab697ff7d85c173370e2813bda9))
* **subscription:** set a default payment method after each successful Stripe payment when none is set ([62abb56](https://github.com/b-partners/bpartners-api/commit/62abb569f9c2fd8e8bb0f42b2f865bf8802b6bc4))



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



