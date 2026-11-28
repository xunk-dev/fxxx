# شاشة Pro فارغة أو تعرض "حاول مرة أخرى" في الاختبار المغلق

هذه القائمة للأسباب التي لا يصلحها الكود. التطبيق لا يفتح Pro عند فشل تحميل الخطط مهما كان السبب، فهو يعرض الرسالة المناسبة ويكتب السبب الحقيقي في السجل.

## كيف تقرأ السبب من التطبيق

من الإعدادات صدّر السجل وابحث عن الأسطر التي تحمل الوسم `billing`.

| السطر في السجل | ما تراه في الشاشة | المعنى |
|---|---|---|
| `offerings failed: reason=NoConnection code=NetworkError` | لا يوجد اتصال | الجهاز بلا إنترنت أو RevenueCat غير قابل للوصول |
| `offerings failed: reason=NoOffering no current offering, offerings=[...]` | الخطط غير متاحة حالياً | لا توجد Offering معلّمة Current في RevenueCat |
| `offerings failed: reason=NoOffering current offering '...' has no purchasable packages` | الخطط غير متاحة حالياً | الـ Offering بلا Packages، أو منتجاتها لم تُجلب من Google Play |
| `offerings failed: reason=NoOffering ... match a plan` | الخطط غير متاحة حالياً | الـ Packages موجودة لكن نوعها أو معرّف منتجها لا يطابق الخطط الثلاث |
| `offerings failed: reason=StoreFailed code=ConfigurationError` | Google Play غير متاح | RevenueCat لم يستطع جلب أي منتج من Google Play (الإعداد أو المنتجات) |
| `offerings failed: reason=StoreFailed code=StoreProblemError` | Google Play غير متاح | مشكلة في خدمات Google Play على الجهاز |
| `offers unavailable: RevenueCat is not configured` | الشراء متاح في نسخة Google Play فقط | المفتاح مفقود أو من نوع `test_` في نسخة release، وسطر `not configured` أعلاه يذكر السبب |
| `customer info ignored: ageMs=...` | لا يتغير شيء | ساعة الجهاز خارج الحد المقبول |

## Google Play Console

- [ ] الاشتراك `frameout_pro` حالته Active (ليس Draft)، وفيه خطتان أساسيتان `monthly` و`yearly` حالة كل منهما Active.
- [ ] المنتج لمرة واحدة `frameout_pro_lifetime` حالته Active وله سعر في بلد المختبر.
- [ ] رُفعت نسخة من التطبيق (AAB موقّع بنفس مفتاح التطبيق) إلى مسار الاختبار المغلق ونُشرت، ورقم الإصدار المثبّت على الجهاز مطابق لها أو أحدث.
- [ ] المنتجات منشورة قبل رفع النسخة أو بعدها بساعات على الأقل، فالمنتجات الجديدة قد تتأخر في الظهور.
- [ ] إيميل المختبر موجود في قائمة المختبرين لمسار الاختبار المغلق، وقبِل دعوة الانضمام من رابط الاختبار.
- [ ] إيميل المختبر مضاف في License testing إن أردت شراءً تجريبياً. هذه المشتريات تُنشئ Entitlement فعلياً في RevenueCat.

## RevenueCat

- [ ] المنتجات الثلاثة مستوردة من Google Play بمعرّفاتها الصحيحة (الاشتراك بخطتيه ومنتج lifetime).
- [ ] Entitlement اسمه بالضبط `frame_out_pro` مربوط بالمنتجات الثلاثة. أي اسم آخر لا يفتح Pro.
- [ ] Offering واحدة على الأقل معلّمة **Current**. اسمها لا يهم، المهم أنها المعلّمة Current، وفيها ثلاث Packages من الأنواع القياسية `$rc_monthly` و`$rc_annual` و`$rc_lifetime`.
- [ ] تطبيق Google Play في RevenueCat مضبوط على الحزمة `com.frameout.app`.
- [ ] مفتاح بناء الإنتاج هو مفتاح SDK العام لتطبيق Google Play ويبدأ بـ `goog_`. مفتاح `test_` يعطّل التهيئة في نسخة release، ومفتاح تطبيق آخر قد يعطي `ConfigurationError`.
- [ ] بيانات حساب الخدمة (Service Account JSON) مرفوعة في RevenueCat وحالتها سليمة، والحساب مدعو في Play Console بصلاحيات عرض معلومات التطبيق وعرض البيانات المالية وإدارة الطلبات والاشتراكات. راجع صفحة إعداد Google Play في توثيق RevenueCat للقائمة الحالية. قد يتأخر سريان الصلاحيات لساعات بعد إضافتها.
- [ ] إشعارات Real-time developer notifications مفعّلة.

## الجهاز

- [ ] التثبيت من Google Play عبر رابط الاختبار المغلق، لا من ملف APK. ملف APK الموقّع بمفتاح مختلف أو غير المطابق لنسخة Play لا يجد المنتجات.
- [ ] حساب Google الأساسي في متجر Play على الجهاز هو نفس إيميل المختبر.
- [ ] تطبيق Google Play وخدماته محدّثان، والجهاز عليه إنترنت.
- [ ] جرّب بعد أي تغيير في اللوحتين بعد دقائق، ثم اضغط "حاول مرة أخرى" أو أعد فتح الشاشة.
