# Web Awesome npm Migration Guide

## Current Status
The app is currently using the **CDN approach** (project script) for Web Awesome. This works immediately but has some limitations (Font Awesome 403 errors).

## To Complete npm Migration

### Step 1: Install the Package
```bash
cd frontend
npm install @awesome.me/webawesome@3.0.0
```

### Step 2: Remove CDN Script
Edit `index.html` and remove this line:
```html
<script src="https://kit.webawesome.com/1313a29d65c14885.js" crossorigin="anonymous"></script>
```

### Step 3: Uncomment CSS Imports
Edit `src/main.tsx` and uncomment these lines:
```typescript
import '@awesome.me/webawesome/dist/styles/webawesome.css'
import '@awesome.me/webawesome/dist/styles/themes/default.css'
```

### Step 4: Uncomment Component Imports
Edit `src/components/webawesome/components.ts` and uncomment all the component imports.

### Step 5: Verify
- Restart the dev server
- Check that Web Awesome components render correctly
- Font Awesome 403 errors should be resolved

## Benefits of npm Approach
- ✅ No CDN dependency
- ✅ Better icon handling (resolves Font Awesome 403 errors)
- ✅ Tree-shaking (only import what you use)
- ✅ Better TypeScript support
- ✅ Faster loading (bundled with app)

## Rollback
If you need to rollback, simply restore the CDN script in `index.html` and comment out the npm imports again.

