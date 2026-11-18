# ملاءمة النصوص لمكانها في كل اللغات

كل نص في التطبيق يمرّ عبر `FrameText` ← `FrameAutoText` (designsystem/components). القاعدة واحدة لكل اللغات:

1. يُعرض النص كما طلبه المستدعي.
2. إذا لم يتسع (عريض جدًا، أسطر أكثر من المسموح، أطول من الارتفاع، أو انكسر سطر في وسط كلمة عادية) يصغر الخط بخطوات حتى 60% وبحدّ أدنى مطلق 9sp. السطر الواحد يقفز مباشرة إلى الحجم الذي يتسع.
3. إذا بقي السطر الواحد لا يتسع يتحرك النص يمينًا ويسارًا (نفس سرعة وتوقف `FrameMarqueeText` مع تلاشي الحواف). في العربية والفارسية والكردية والأردية يبدأ من اليمين. عدة أسطر أو «تقليل الحركة» تنتهي بثلاث نقاط.
4. إذا اتسع المكان لاحقًا (تدوير الشاشة، نافذة أكبر) يكبر الخط من جديد.

## الاستخدام
```kotlin
FrameText(text, style, color, maxLines = 1)                    // الافتراضي: Auto (تصغير ثم حركة)
FrameText(text, style, color, fit = FrameFit.ShrinkOnly)        // تصغير فقط
FrameText(text, style, color, fit = FrameFit(minSp = 8f))       // حدّ أدنى مختلف (تسميات صغيرة)
FrameText(text, style, color, fit = FrameFit.None)              // بلا تدخل (أرقام متحركة، نص يقيسه غيرك)
FrameAutoText(annotatedString, style)                           // نص منسّق (فرشاة، روابط)
```
يعمل فقط عندما يكون العرض محدودًا (weight، fillMaxWidth، width ثابت، أو ما تبقى في Row).

## استثناءات مقصودة
`FitParagraph` (قياس دقيق لبطاقة Pro)، `FrameRollingText`، `FrameMarqueeText`، رسوم الكانفاس الزخرفية (`HomeBentoArt`، `ProBadge`)، ونص العروض البعيدة `PromoSlot`.

## الفحص
`python3 scripts/audit_text_fit.py . build/strings_width_audit.csv` يرتّب كل نص بكل لغة حسب عرضه مقابل الإنجليزية. الأعلى نسبة هو الأولى بالمعاينة على الجهاز (الألمانية والفرنسية والإسبانية والإيطالية والفلبينية أطول بنحو 25–30% في المتوسط).
