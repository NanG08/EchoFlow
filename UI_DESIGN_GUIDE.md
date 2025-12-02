# EchoFlow - UI Design Guide

## 🎨 Visual Design System

### Color Palette

#### Primary Colors

```
Electric Teal (Primary)
#14B8A6 ████████ Main accent color
#0D9488 ████████ Darker variant (hover/pressed)
#2DD4BF ████████ Lighter variant (highlights)
```

#### Neutral Colors (Light Mode)

```
Background
#FAFAFA ████████ Main background (off-white)
#FFFFFF ████████ Surface/Cards (pure white)
#F5F5F5 ████████ Surface variant

Text Colors
#1F1F1F ████████ Primary text (soft black)
#6B7280 ████████ Secondary text (gray)
#9CA3AF ████████ Tertiary text (light gray)

Dividers
#E5E5E5 ████████ Subtle lines
```

#### Neutral Colors (Dark Mode)

```
Background
#121212 ████████ Main background (true black)
#1E1E1E ████████ Surface/Cards (dark gray)
#2A2A2A ████████ Surface variant

Text Colors
#F5F5F5 ████████ Primary text (off-white)
#B0B0B0 ████████ Secondary text (light gray)
#808080 ████████ Tertiary text (gray)

Dividers
#3A3A3A ████████ Subtle lines
```

#### State Colors

```
Success
#10B981 ████████ Green (confirmations)

Error
#EF4444 ████████ Red (errors, stop button)

Warning
#F59E0B ████████ Amber (warnings)
```

### Typography Scale

```
App Name / Hero
Font: Sans-serif Medium
Size: 28sp
Color: Text Primary
Letter Spacing: 0.02

Tagline
Font: Sans-serif Regular
Size: 13sp
Color: Text Secondary

Section Headers
Font: Sans-serif Medium
Size: 12sp
Color: Text Tertiary
Letter Spacing: 0.05
Transform: Uppercase (optional)

Body Text
Font: Sans-serif Regular
Size: 16sp
Color: Text Primary
Line Height: 24sp (1.5x)

Button Text
Font: Sans-serif Medium
Size: 14-16sp
Color: Primary or White
Letter Spacing: 0

Caption Text
Font: Sans-serif Regular
Size: 12sp
Color: Text Secondary
```

### Spacing System

```
Micro:   4dp  ▪
Small:   8dp  ▪▪
Medium:  12dp ▪▪▪
Base:    16dp ▪▪▪▪
Large:   20dp ▪▪▪▪▪
XLarge:  24dp ▪▪▪▪▪▪
XXLarge: 32dp ▪▪▪▪▪▪▪▪
```

### Component Specifications

#### Cards

```
Corner Radius: 16dp
Elevation: 2dp
Padding: 20dp
Background: Surface
Margin: 16dp between cards
```

#### Buttons

**Primary Button**

```
Height: 56dp
Corner Radius: 28dp (fully rounded)
Background: Primary (#14B8A6)
Text: White, 16sp, Medium
Icon: 24dp, White
Padding: 16dp horizontal
Elevation: 4dp
```

**Outlined Button**

```
Height: 42-48dp
Corner Radius: 12dp
Background: Transparent
Border: 1dp, Divider color
Text: Text Primary, 14sp
Padding: 16-20dp horizontal
```

**Selected State (Mode Buttons)**

```
Background: Primary (#14B8A6)
Text: White
Border: None
```

**Text Button**

```
Height: wrap_content
Background: Transparent
Text: Primary, 14sp
Ripple: 20% Primary
```

#### Input Fields

```
Height: 56dp
Corner Radius: 12dp
Background: Surface Variant
Border: 1dp, Divider (focused: Primary)
Text: 16sp
Padding: 16dp
```

#### Toggle Switch

```
Thumb: White
Track: Primary (on), Gray (off)
Size: Standard (Material)
```

### Layout Structure

#### Screen Layout

```
┌─────────────────────────────────────┐
│  Header Section                     │
│  - App branding                     │
│  - Language selection               │
│  - Wake word toggle                 │
│  Background: Surface                │
│  Padding: 24dp horizontal           │
│  Elevation: 2dp                     │
├─────────────────────────────────────┤
│  Mode Selector (Horizontal Scroll)  │
│  - Voice, Camera, Photo, etc.       │
│  Padding: 16dp                      │
│  Gap: 10dp between buttons          │
├─────────────────────────────────────┤
│                                     │
│  Content Area (Scrollable)          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Original Text Card          │   │
│  │ - Label                     │   │
│  │ - Text content              │   │
│  │ Radius: 16dp, Elevation: 2dp│   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Translation Card            │   │
│  │ - Label                     │   │
│  │ - Text content              │   │
│  │ Background: Primary Light   │   │
│  │ Radius: 16dp, Elevation: 2dp│   │
│  └─────────────────────────────┘   │
│                                     │
│  Padding: 20dp                      │
│  Gap: 16dp between cards            │
├─────────────────────────────────────┤
│  Bottom Action Section              │
│  - Main action button (Start/Stop)  │
│  - Secondary actions (History/...)  │
│  Background: Surface                │
│  Padding: 20dp horizontal           │
│  Elevation: 8dp                     │
└─────────────────────────────────────┘
```

### Visual Hierarchy

```
Level 1: Primary Actions
- Main Start/Stop button
- Size: 56dp height
- Color: Primary teal
- Position: Bottom, prominent

Level 2: Mode Selection
- Mode buttons (Voice, Camera, etc.)
- Size: 42dp height
- Color: Outlined or Selected
- Position: Below header

Level 3: Content Cards
- Original and Translation cards
- Size: Variable (min 120dp)
- Color: Surface or Primary Light
- Position: Main content area

Level 4: Secondary Actions
- History, Settings buttons
- Size: 44dp
- Color: Text buttons
- Position: Bottom corners
```

### Animation Specifications

#### Entrance Animations

```kotlin
// Fade + Slide Up
Duration: 400ms
Interpolator: Decelerate
Delay: Staggered by 100ms

// Scale In
Duration: 300ms
Scale: 0.8 → 1.0
Interpolator: OvershootInterpolator
```

#### State Changes

```kotlin
// Mode Selection
Duration: 200ms
Property: Background, Text Color
Interpolator: FastOutSlowIn

// Button Press
Duration: 100ms
Scale: 1.0 → 0.95 → 1.0
Ripple: 300ms
```

#### Text Updates

```kotlin
// Pulse Animation
Duration: 300ms
Scale: 1.0 → 1.05 → 1.0
Alpha: 1.0 → 0.8 → 1.0
```

### Iconography

#### Style

- Outlined style (Material Icons)
- Stroke Width: 2dp
- Size: 24dp (standard), 20dp (small)
- Color: Primary or Text color

#### Icons Used

```
Microphone: ic_mic.xml (Start recording)
Stop: ic_stop.xml (Stop recording)
Swap: ic_swap.xml (Language swap)
Camera: ic_camera.xml (Camera mode)
Photo: ic_photo.xml (Gallery)
History: ic_history.xml (Past translations)
Settings: ic_settings.xml (Configuration)
```

### Shadows & Elevation

```
Surface Level 0 (Background)
Elevation: 0dp
Shadow: None

Surface Level 1 (Cards)
Elevation: 2dp
Shadow: Subtle, 8% opacity

Surface Level 2 (Header)
Elevation: 2-4dp
Shadow: Light, 10% opacity

Surface Level 3 (FAB / Bottom Bar)
Elevation: 6-8dp
Shadow: Medium, 15% opacity

Surface Level 4 (Dialogs)
Elevation: 16dp
Shadow: Strong, 20% opacity
```

### Accessibility

#### Touch Targets

```
Minimum: 44dp × 44dp
Recommended: 48dp × 48dp
Primary Action: 56dp × wrap_content
```

#### Contrast Ratios

```
Text on Background
Large Text (18sp+): ≥ 3:1
Normal Text (16sp): ≥ 4.5:1

Interactive Elements
Primary Button: ≥ 3:1
Icons: ≥ 3:1
```

#### Screen Reader

```
All buttons: contentDescription
Images: Descriptive labels
State changes: Announced
Live regions: For dynamic content
```

### Responsive Design

#### Phone (Portrait)

```
Width: 360-420dp
Layout: Single column
Mode selector: Horizontal scroll
Cards: Full width (-40dp margin)
```

#### Phone (Landscape)

```
Width: 640-900dp
Layout: Adjusted padding
Mode selector: Fits on screen
Cards: Max width 600dp
```

#### Tablet

```
Width: 600dp+
Layout: Centered, max 720dp
Mode selector: Centered
Cards: Side-by-side option
Padding: 32dp+
```

### Dark Mode

#### Automatic Switching

```kotlin
// Follow system setting
android:theme="@style/Theme.EchoFlow"
Configuration.UI_MODE_NIGHT_YES/NO
```

#### Color Adaptations

```
Backgrounds: Darker (#121212)
Surfaces: Elevated (#1E1E1E)
Primary: Brighter teal (#2DD4BF)
Text: Inverted (White → Black)
Shadows: More pronounced
```

#### Elevation in Dark Mode

```
Level 1: +5% white overlay
Level 2: +7% white overlay
Level 3: +9% white overlay
Level 4: +12% white overlay
```

## 🎯 Design Checklist

### Visual Consistency

- [x] Single accent color (teal) used throughout
- [x] Consistent corner radius (12-16dp)
- [x] Uniform spacing (16-24dp system)
- [x] Same typography scale
- [x] Matching elevation levels

### Minimal Design

- [x] Generous whitespace
- [x] Simple iconography
- [x] Clean typography
- [x] Limited color palette
- [x] Focused content

### Modern Aesthetic

- [x] Material Design 3
- [x] Rounded corners
- [x] Subtle shadows
- [x] Smooth animations
- [x] Contemporary fonts

### User Experience

- [x] Clear hierarchy
- [x] Easy navigation
- [x] Large touch targets
- [x] Instant feedback
- [x] Accessible design

## 📐 Implementation Examples

### Card Component

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="@color/surface">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">
        
        <!-- Content here -->
        
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### Primary Button

```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.EchoFlow.Button"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="@string/action_start"
    android:backgroundTint="@color/primary"
    app:cornerRadius="28dp"
    app:icon="@drawable/ic_mic"
    android:elevation="4dp" />
```

### Mode Button

```xml
<Button
    style="@style/Widget.EchoFlow.Button.Outlined"
    android:layout_width="wrap_content"
    android:layout_height="42dp"
    android:paddingHorizontal="20dp"
    android:text="@string/mode_voice"
    android:background="@drawable/button_mode_selector"
    android:textColor="@color/button_mode_text_color" />
```

---

**Design System Complete** ✨

All components follow the minimal, modern aesthetic with electric teal accent.

[Back to Implementation Summary](IMPLEMENTATION_SUMMARY.md) • [Build Checklist](BUILD_CHECKLIST.md)
