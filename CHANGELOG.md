# [0.105.0](https://github.com/b-partners/bpartners-api/compare/v0.104.0...v0.105.0) (2026-11-23)


### Bug Fixes

* **area-picture:** refuse an annotation on an unknown area picture with a 404 ([d306098](https://github.com/b-partners/bpartners-api/commit/d30609854d66425d617da4bf32aacb51870933ce))
* **bridges:** forward query values as received through one shared relay  ([bcb5178](https://github.com/b-partners/bpartners-api/commit/bcb5178aa1f28602f1b4b5a6fc487489c62828dd))
* **bridges:** no empty 400 on unparseable query params ([9a60167](https://github.com/b-partners/bpartners-api/commit/9a6016780eb063f71c00b5f0704b8b57dbfb4f1a))
* **db:** backfill analysis projects 2D-3D steps from draft geo sessions ([e0c9ad5](https://github.com/b-partners/bpartners-api/commit/e0c9ad565861ddd61c42967b15b11b78f553c3bf))
* **detection-tracking:** register the 2D on its project even when its zone is already known ([b2b1ea6](https://github.com/b-partners/bpartners-api/commit/b2b1ea62b43cbda8aae84d2ca55ee02fbe1d5d15))
* **geo-jobs-bridge:** forward the analysis project of detection and 3D requests ([b1707d7](https://github.com/b-partners/bpartners-api/commit/b1707d79da3286914f739d475eb371b3b947f079))
* grant credit when period >= last days of month billed ([5d72aae](https://github.com/b-partners/bpartners-api/commit/5d72aae1601aeb50775d0d8ab2143dae8237abed))
* **llm:** etatApparent is a string ([82b6607](https://github.com/b-partners/bpartners-api/commit/82b6607aad8f72f46598d4e1cb8c920b996dd6cd))
* **logs:** keep api keys out of toString ([d208d53](https://github.com/b-partners/bpartners-api/commit/d208d533d1d36e3edb8e7e0822988a96fc598b69))
* **mail:** use the Angus provider for jakarta.mail 2.1 and jakarta.activation 2.1 ([042049a](https://github.com/b-partners/bpartners-api/commit/042049a43366fd0b9ac1e7e1a5de55ce8df3b22e))
* **onboarding:** reject emails already used in database or Cognito ([7a124c1](https://github.com/b-partners/bpartners-api/commit/7a124c1998eeeca85f68c22998a24042b842dc66))
* record user activity on 3D model registration ([6bae713](https://github.com/b-partners/bpartners-api/commit/6bae71345b80521754fd078a8100bb2816a09d49))
* **subscription:** compute termination amount check with VAT stored in basis points ([ea155a6](https://github.com/b-partners/bpartners-api/commit/ea155a6696923453daba3ce3e8571cb49fb90fa9))
* **subscription:** terminate subscriptions sequentially to avoid invoice reference collisions ([c3b27b8](https://github.com/b-partners/bpartners-api/commit/c3b27b8b9686dfd8d6820c1f00d283acf3de9a55))
* **user:** keep subscription products when saving a user ([490092d](https://github.com/b-partners/bpartners-api/commit/490092d8c028540f743a3c86843cc5cf22fbae15))


### Features

* bridge geodata lidar caching ([fdf6ba5](https://github.com/b-partners/bpartners-api/commit/fdf6ba53b13d88e46e77a91a8773af4878737f61))
* **export-pdf:** hide measurements and show a replacement text ([c877600](https://github.com/b-partners/bpartners-api/commit/c87760053fb1dedfbe2e45f6379c97ad90917d25))
* list subscription invoice summaries ([58e6f3f](https://github.com/b-partners/bpartners-api/commit/58e6f3f37991c941f75d5a71fae85a5aa1d243c0))
* **subscription:** sync Stripe-only subscriptions and issue their invoice without mail ([c64a03e](https://github.com/b-partners/bpartners-api/commit/c64a03e374bfbe5122683af920d5d834e27b3817))
* **subscription:** terminate subscription and issue credit note on Stripe cancellation ([27a0292](https://github.com/b-partners/bpartners-api/commit/27a02922fb166a14bd9c244afcd2090eba835f1f))
* **subscription:** terminate subscriptions and issue credit notes from admin trigger ([ef3d29f](https://github.com/b-partners/bpartners-api/commit/ef3d29fdff86dbddd745758591a05b985dd72cca))



# [0.104.0](https://github.com/b-partners/bpartners-api/compare/v0.103.0...v0.104.0) (2026-11-04)


### Features

* bridge genai roof report ([220fbd4](https://github.com/b-partners/bpartners-api/commit/220fbd419af3d2cf7c3346803fd67d31b4c49fbb))
* bridge geo-jobs endpoints ([a7a6e1d](https://github.com/b-partners/bpartners-api/commit/a7a6e1dae9a1ed9a1f670910d0dfe1820cd0906b))
* bridge geodata service ([be73cc2](https://github.com/b-partners/bpartners-api/commit/be73cc2a9d8c355645e4ebe78214c35438e928d6))
* bridge GeoServer WMS GetMap ([b017a77](https://github.com/b-partners/bpartners-api/commit/b017a77b416ed80c4f57ef7e5c725ec425f2b48a))
* draft analysis project ([46dfbbf](https://github.com/b-partners/bpartners-api/commit/46dfbbf03613c97f1ba30c54d29ca4da2419ab02))
* **security:** dashboard token auth for bridge endpoints ([02e8f58](https://github.com/b-partners/bpartners-api/commit/02e8f586149688f2f6aa2470f46d5c6ecd3c393d))



# [0.103.0](https://github.com/b-partners/bpartners-api/compare/v0.102.0...v0.103.0) (2026-11-04)


### Bug Fixes

* **subscription:** allow a single free trial per user across all plans ([afb52e7](https://github.com/b-partners/bpartners-api/commit/afb52e7548c9f06970a90a6bb683bbbd70eec6f8))
* **subscription:** attach uncommitted monthly renewals to the invoice already covering them ([6760e21](https://github.com/b-partners/bpartners-api/commit/6760e21185d8ca7a2b4531e7d08d870a424ac7c7))


### Features

* analysis project credit ([b9c4058](https://github.com/b-partners/bpartners-api/commit/b9c40584aa617a70ab49701939677ed5b95a8eb9))
* **event:** api keys repair ([6a9900b](https://github.com/b-partners/bpartners-api/commit/6a9900b83a236b8917852111d2bf30924edd8ddf))



# [0.102.0](https://github.com/b-partners/bpartners-api/compare/v0.101.0...v0.102.0) (2026-10-29)


### Features

* **subscription:** send credit note mail without attachment and point to the dashboard ([b5d6ebc](https://github.com/b-partners/bpartners-api/commit/b5d6ebcb0811458527a5b5e5caa5fb0379fa11d6))
* **subscription:** share subscription and credits with secondary users ([3b2d032](https://github.com/b-partners/bpartners-api/commit/3b2d032196b45a43cb3f3c2e0d6585a241a00ef7))



# [0.101.0](https://github.com/b-partners/bpartners-api/compare/v0.97.0...v0.101.0) (2026-10-22)


### Bug Fixes

* ajust subscription invoice mail content according yearly billing period ([4bc5572](https://github.com/b-partners/bpartners-api/commit/4bc557212db9baf2ac4cbc3332101d992b5b3911))
* bill annual subscriptions as monthly lines with a discount column ([9f0db33](https://github.com/b-partners/bpartners-api/commit/9f0db330177ff9b9172de4c1b9805ef7f0c21174))
* **billing-stats:** exclude refunded subscription payments ([b2dd5c3](https://github.com/b-partners/bpartners-api/commit/b2dd5c39ea3661a99c94175d6ff48b0643df6c60))
* compute subscription invoice discount from either annual_discount_percent or from net/gross price ([581c725](https://github.com/b-partners/bpartners-api/commit/581c72548d01d601171e706bf83e747ca18a23dc))
* **db:** force unit prices for usage based and legasy essential ([b1f0b4f](https://github.com/b-partners/bpartners-api/commit/b1f0b4f5ef52379efe854721457b79507bd49346))
* distinguish active subscriptions (stock) from sales flow (new subs by start, billed instalments by payment) in subscriptionBillingStats charts ([5650ba0](https://github.com/b-partners/bpartners-api/commit/5650ba02426000d05a0dfe633fd8c3d111c5e0d5))
* **export-pdf:** guard edge lists against index out of bounds and log mismatches ([f887b0a](https://github.com/b-partners/bpartners-api/commit/f887b0adcbd913633938f84eb4547fc234accdbf))
* grant subscription credit based on the billing month - not payment date ([d62abba](https://github.com/b-partners/bpartners-api/commit/d62abbab7d5f7e11a1d7733b4b15150fe3205ce3))
* handle subscription billing stats on MVC ([eb8ab92](https://github.com/b-partners/bpartners-api/commit/eb8ab921a9de9f47d21c7d50b4de7e3dcfc1f654))
* ignore historic susbcribed user trial period ([249a4e1](https://github.com/b-partners/bpartners-api/commit/249a4e1e6a11923ada93c8cb7f88a8c9b574ab0f))
* **invoice:** ajust invoice margin ([f9a3509](https://github.com/b-partners/bpartners-api/commit/f9a35099d31a347d170ea7ddf4c1063c514e4f90))
* **invoice:** bill monthly subscriptions as a single instalment schedule invoice with excluded already invoiced period ([3b1a6bd](https://github.com/b-partners/bpartners-api/commit/3b1a6bdf89cb251250ddfd27cda5a7a2ae389958))
* **invoice:** bound the annual billed period to one year so a subscription started on a month last day does not bill a thirteenth full month ([5283d1d](https://github.com/b-partners/bpartners-api/commit/5283d1dc5390983e20cf65bcadaf3960016f2398))
* **invoice:** ignore a monthly payment billing a period that starts after its own payment date ([c14af23](https://github.com/b-partners/bpartners-api/commit/c14af2395d66f075f8256efbb1025aaafa4c7c5b))
* **invoice:** keep the monthly instalment schedule when an already invoiced period runs past the commitment ([1d29b1e](https://github.com/b-partners/bpartners-api/commit/1d29b1eebbd21550d190950d435fd1635bc78234))
* **invoice:** price the first prorated subscription line with the amount charged by Stripe ([8a709cd](https://github.com/b-partners/bpartners-api/commit/8a709cde09e57dbeb9eeef186df448e80ca83886))
* **invoice:** prorate a partial annual subscription line on its own calendar month length instead of the total partial days ([438b30c](https://github.com/b-partners/bpartners-api/commit/438b30ceb4a1174c9d26554e00cdd4ac511d0fe7))
* **invoice:** read the commitment end as an exclusive bound when computing the last full billed month ([08819e5](https://github.com/b-partners/bpartners-api/commit/08819e5f929c16b79d13ce0580adfe054ba8082b))
* **invoice:** title the fallback invoice with its payment date when the billed period is not credible ([a82b980](https://github.com/b-partners/bpartners-api/commit/a82b9807dcbb1028bc0cb626651a734c9b249e42))
* read annual_discount_percent as hundredths of percent (1000=10%) so the declared discount drives the invoice ([cf9cfc2](https://github.com/b-partners/bpartners-api/commit/cf9cfc2afc8963a705e33d3f058dfb97a5ddc1dd))
* reconstruct annual invoice discount column from catalog monthly price when annualDiscountPercent is unset ([521b15f](https://github.com/b-partners/bpartners-api/commit/521b15f9db019eb40add403a5f519c426c12d0b1))
* set DEFAULT_OVERAGE_UNIT_PRICE_IN_CENTS=1000 ([d648a6c](https://github.com/b-partners/bpartners-api/commit/d648a6c8076250d7d16bcdfc439f3ec809cdb992))
* **stripe-subscriptions:** allow sending ALREADY_INVOICED mail ([057c154](https://github.com/b-partners/bpartners-api/commit/057c15458b36b1b99e246fec7edd783b4287103f))
* **stripe-subscriptions:** avoid invoice reference collision and isolate backfill failures ([8ae5c4e](https://github.com/b-partners/bpartners-api/commit/8ae5c4e13f27da3d6a79647e6e73e993bb6ae2e7))
* **stripe-subscriptions:** stop resending mail for already invoiced payments and drop paid mail template ([500c6e5](https://github.com/b-partners/bpartners-api/commit/500c6e53f3bf90f4737af34ac34f03ac7910ffd4))
* **subscription-billing-stats:** active subscriptions as cumulative net evolution from new subs baseline ([030843b](https://github.com/b-partners/bpartners-api/commit/030843bf67e4a8509bfb66ec0ed94f426fe92e17))
* **subscription:** record the billed period and plan from the subscribed stripe line rather than a proration line ([2afc2d3](https://github.com/b-partners/bpartners-api/commit/2afc2d33193f89cad1c2d42856819c3289b8462e))
* **subscription:** trigger the remaining period invoicing from an admin endpoint instead of a scheduler ([372213a](https://github.com/b-partners/bpartners-api/commit/372213ac4da1ac9d318f09a1e8f4dfe8eb113c22))


### Features

* **account-holder:** cc other concerned email on prospect notification ([7b27ffe](https://github.com/b-partners/bpartners-api/commit/7b27ffed0e50ffe0184f8a1acaf2e1c82fa623a3))
* compact subscription invoice layout to fit a single page without affecting standard invoices ([a902a55](https://github.com/b-partners/bpartners-api/commit/a902a55c5fc6ce7103eba9684ba02d55aa59c54e))
* confirmable subscription upgrade sessions with prorated credit ([46f5e14](https://github.com/b-partners/bpartners-api/commit/46f5e145aad9c48e2e92cf28bffd2bddf0cf6a97))
* **event:** check users' analysis api keys against geo-jobs ([ff7e3c4](https://github.com/b-partners/bpartners-api/commit/ff7e3c428d732e0accdb5db13370c43dd19da993))
* **event:** split analysis key check chain into gradually sent chunks ([9ee9d5c](https://github.com/b-partners/bpartners-api/commit/9ee9d5c68896cb1ac8ab8aa6af35a672d32c541b))
* export pdf style V2 ([ffb5108](https://github.com/b-partners/bpartners-api/commit/ffb5108498c47bc42a74befd76992e2e37b43cae))
* export sent subscription invoices for SUBSCRIPTION_INVOICE_EXPORTER and ADMIN roles ([d4d7c7a](https://github.com/b-partners/bpartners-api/commit/d4d7c7a581d8fb97f44cd7bd391181e5197fb8f4))
* **export-pdf:** make degradation rate section optional ([fea7b94](https://github.com/b-partners/bpartners-api/commit/fea7b94d5b7d9c28f5864057b4ef0e83fa9ea94c))
* **invoice:** bill annual full months at catalog price and absorb rounding on the last line ([9509edd](https://github.com/b-partners/bpartners-api/commit/9509eddd061e591c2f42c98e098a833a5393bdb8))
* **map-layers:** add GET /map/layers proxying geodata with its api key ([c05f9dc](https://github.com/b-partners/bpartners-api/commit/c05f9dc1780a2139037e72a481d67b7df5990623))
* **map-layers:** add GET /map/layers/actual proxying geodata with its api key ([e75082d](https://github.com/b-partners/bpartners-api/commit/e75082dd194d19398ed6424d302dcc28bd28b198))
* name the plan on annual invoice lines and prorate first/last month on calendar months ([37289c0](https://github.com/b-partners/bpartners-api/commit/37289c076310a849ff455f733b680b4bd0a6d41b))
* **onboarding:** add promo code system ([858f6f0](https://github.com/b-partners/bpartners-api/commit/858f6f0386b42ef47b198a9a3276272d905207cf))
* **prospect:** upload notification attachments through pre-signed S3 URLs ([577932f](https://github.com/b-partners/bpartners-api/commit/577932f3e89b297d50e818a881bde55384e61a2c))
* **subscription:** issue one invoice per commitment period and backfill the period left on running commitments ([fef8663](https://github.com/b-partners/bpartners-api/commit/fef8663c0af75684f086bef7bd1be2528e24f197))


### Performance Improvements

* **event:** skip the classpath scan for the analysis api key check events ([c013f6d](https://github.com/b-partners/bpartners-api/commit/c013f6db7fa4c784d7d8af646008bb47e5cd59ed))



# [0.97.0](https://github.com/b-partners/bpartners-api/compare/v0.96.0...v0.97.0) (2026-09-18)


### Features

* allow listing UNPAID subscriptionInvoices without yearMonth ([2acf61f](https://github.com/b-partners/bpartners-api/commit/2acf61fcee14a1b7b05f6c293d0cfab74fd995a2))



# [0.96.0](https://github.com/b-partners/bpartners-api/compare/v0.94.0...v0.96.0) (2026-09-18)


### Bug Fixes

* add payment status and payment URL on subscriptionInvoice ([a4f6272](https://github.com/b-partners/bpartners-api/commit/a4f6272a39ee873429b18f0caf00ca2231735215))
* **billing stats HTML:** serve text/html as UTF-8 ([4185286](https://github.com/b-partners/bpartners-api/commit/4185286d8ade5babddfc2ebdb96bba76f2b49307))
* consume granted credits on analysis during free trial ([2b95b6f](https://github.com/b-partners/bpartners-api/commit/2b95b6f10096800fafe06cd756d3ff398a320481))
* **db:** exclude Module devis automatisé from PRO ([b7ca3ec](https://github.com/b-partners/bpartners-api/commit/b7ca3ecf7699460b80e670de918e6ff920494f60))
* **db:** remove deprecated entries from susbcription_products comparison and features ([0b61f6a](https://github.com/b-partners/bpartners-api/commit/0b61f6a33a455e3c100050cd37e6e01db2231796))
* **DetectionTrackingService:** persist detection tracking zone as address log ([326f0ff](https://github.com/b-partners/bpartners-api/commit/326f0ffd2780df6eb33f3bd76a8fcdb4f05ea28b))
* **DetectionTrackingService:** skip debit when zone already registered per user ([447f271](https://github.com/b-partners/bpartners-api/commit/447f271a968c34c2b265eae2a36523fc45136790))
* handle billing statistics on HTML ([b20001c](https://github.com/b-partners/bpartners-api/commit/b20001c2d5725c1eb17828e802a1e87d343e593c))
* include featureSections and comparisonEntries on stripe product concilliation ([de16bac](https://github.com/b-partners/bpartners-api/commit/de16bac0cb71941b1ea0ee40db343da5023195a6))
* include trial analysis granted on stripe product concilliation ([5160058](https://github.com/b-partners/bpartners-api/commit/5160058f4b3f4f67128f856d6269f517977fbc0f))
* reconcile subscription invoice with stripe by amount and period to disambiguate several unpaid invoices ([1d40436](https://github.com/b-partners/bpartners-api/commit/1d40436ac6da2f18446b5a3be253e4e7085cafff))
* return credit balance on GET /users V2 ([bf7ee64](https://github.com/b-partners/bpartners-api/commit/bf7ee64c7eecddde61bd787e18a0cec4e6ea886a))
* return credit balance on GET /users V2 ([466e7e2](https://github.com/b-partners/bpartners-api/commit/466e7e22b9de49f61d890ee6891682c03a963bc4))
* **SubscriptionService:** do not consider UNPAID as active susbcription ([c5be374](https://github.com/b-partners/bpartners-api/commit/c5be374700239d197074d2d50794ea55411d1821))
* **SubscriptionService:** do not consider UNPAID as active susbcription ([db0f34a](https://github.com/b-partners/bpartners-api/commit/db0f34aa329fe1fa93b11d6cf612657c5ea8858e))
* **SubscriptionStatus:** map incomplete_expired as CANCELED not UNPAID ([8add6e5](https://github.com/b-partners/bpartners-api/commit/8add6e5f17084c2812aefba9b4e2561885f5c026))
* **SubscriptionStatus:** map incomplete_expired as CANCELED not UNPAID ([56e8e3a](https://github.com/b-partners/bpartners-api/commit/56e8e3aaa31bca3132471628d145df2e8dd4fe4c))
* transmit user dashboard api key to analysis api and keep it on analysis key creation ([af039ba](https://github.com/b-partners/bpartners-api/commit/af039ba4daf6eaed5120dc8fb02a8e3a83346983))
* transmit user dashboard api key to analysis api and keep it on analysis key creation ([e9bc436](https://github.com/b-partners/bpartners-api/commit/e9bc4361befbe39812c74763eada733e18ad2098))


### Features

* add admin subscription billing stats endpoint ([64a8416](https://github.com/b-partners/bpartners-api/commit/64a8416e489cebaa2fe91307f2f009a351dc67eb))
* persist mutation and fireRisk on area picture annotation metadata ([677608a](https://github.com/b-partners/bpartners-api/commit/677608a9aa0bff452715b8fd9eb285a9b36b9070))
* support fileId as an alternative to url in export pdf ImageSection ([74eeea5](https://github.com/b-partners/bpartners-api/commit/74eeea53a172fafde4d8cbe7916fadb7c56e37df))



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



