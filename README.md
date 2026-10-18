# HariboteAirCraft (HAC) Remake - Minecraft 1.12.2 Forge

> **Opis:** wrzucam na githuba bo ten typ boi się pobrać z discorda

Wierny i zoptymalizowany remake legendarnej japońskiej modyfikacji **HariboteAirCraft (HAC)** stworzony dla Minecraft **1.12.2 Forge** z oryginalnymi teksturami, modelami i mechanikami.

**Autor Remake:** Patryk1_1  
**Oryginalny mod:** HariboteAirCraft autorstwa waco/yamato  

---

## Nowości i cechy remaku

1. **Oryginalne tekstury i modele bloków:**
   - Wszystkie oryginalne tekstury Pyxis (`py_bottom.png`, `py_side.png`, `py_top0..3.png`, `py_rotor.png`).
   - Dwublokowy model Pyxis ze wskaźnikiem kompasu obracającym się zależnie od kierunku postawienia.
   - Płynnie obracający się wirnik w locie.

2. **Zaawansowana fizyka i kolizje (Chodzenie po pokładzie w locie):**
   - Pełna obsługa `GetCollisionBoxesEvent` w Forge: **każdy pojedynczy blok statku ma rzeczywistą fizyczną kolizję**.
   - Pasażerowie mogą swobodnie chodzić, biegać i skakać po pokładzie statku w trakcie lotu (idealne pod multiplayer).
   - Inteligentne prowadzenie pasażerów (`deck carrier`), zapobiegające spadaniu z pokładu przy skrętach i manewrach.

3. **Intuicyjne sterowanie:**
   - `W` / `S` - Przód / Tył
   - `A` / `D` - Lot na boki (Strafe lewo / prawo)
   - `Ruch myszką` - Płynne skręcanie i obracanie statku
   - `Spacja` - Wznoszenie
   - `C` lub `Ctrl` - Opadanie
   - `R` - Lądowanie (bezpieczna materializacja z powrotem do świata)

4. **Multiplayer & Komendy:**
   - Wieloosobowość: wsparcie dla wielu pasażerów.
   - Komenda `/HariboteAirCraft` w grze zwracająca informacje o autorze remaku: `Patryk1_1`.

5. **Receptura rzemieślnicza (Pyxis):**
   - Kompas (góra środek)
   - Diamenty (po bokach)
   - Blok złota (środek)
   - Obsydian (dolny rząd)

---

## Jak zbudować projekt ze źródeł

Projekt korzysta z **RetroFuturaGradle (RFG)**.

Aby zbudować plik `.jar`:
```bash
./gradlew build
```
Gotowy mod znajdziesz w folderze `build/libs/`.
