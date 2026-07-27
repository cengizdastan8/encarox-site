# Encarox Ajans Sitesi

MIT lisanslı [AstroWind](https://github.com/arthelokyo/astrowind) temel alınarak Encarox için özelleştirilmiş, statik ve SEO odaklı ajans sitesi.

## Yerel çalışma

```powershell
npm install
npm run dev
```

## Doğrulama ve production build

```powershell
npm run check:astro
npm run check:eslint
npm run build
```

Build çıktısı `dist/` klasörüne yazılır. Site analitik veya iletişim formu kullanmaz; ana CTA geçici olarak bağlı GitHub profiline gider.

## Yayın öncesi

- Gerçek domain alındığında `src/config.yaml` içindeki `site` değerini güncelle.
- Telefon veya kurumsal e-posta belli olduğunda ana sayfadaki iletişim CTA'sını güncelle.
- Domain doğrulandıktan sonra Google Search Console doğrulama kimliğini ekle.
