# RadaeePDF SDK Master for Android

<img src="https://www.radaeepdf.com/wp-content/uploads/2024/08/solo_butterly_midres.png" style="width:100px;"> 

RadaeePDF SDK is a powerful, native PDF rendering and manipulation library for Android applications. Built from true native C++ code, it provides exceptional performance and a comprehensive set of features for working with PDF documents.

## About RadaeePDF

RadaeePDF SDK is designed to solve most developers' needs with regards to PDF rendering and manipulation. The SDK is trusted across industries worldwide including automotive, banking, publishing, healthcare, and more.

### Key Features

- **PDF ISO32000 Compliance** - Full support for the widely-used PDF format standard
- **High Performance** - True native code compiled from C++ sources for optimal speed
- **Annotations** - Create and manage text annotations, highlights, ink annotations, and more
- **Protection & Encryption** - Full AES256 cryptography for document security
- **Text Handling** - Search, extract, and highlight text with ease
- **Form Editing** - Create, read, and write PDF form fields (AcroForms)
- **Digital Signatures** - Sign and verify PDF documents with digital certificates
- **Multiple View Modes** - Single page, continuous scroll, and more
- **Night Mode** - Built-in dark mode support for better readability

## Quick Start - Run Demo

To quickly test the RadaeePDF SDK demo:

1. Open Android Studio
2. Click on **Clone Repository** (or File → New → Project from Version Control)
3. Paste the repository URL:
   ```
   https://github.com/RadaeePDF-Jugaad/RadaeePDF-Master-Android.git
   ```
4. Click **Clone** and wait for the project to open
5. Open the `radaeepdfmaster` folder in the project structure (if needed)
6. Click the Play/Run button (▶) to run the demo in an emulator or connected device

## Installation

### Manual Installation

1. Download the RadaeePDF SDK library from Git Repository
   ```
   https://github.com/RadaeePDF-Jugaad/RadaeePDF-Master-Android.git
   ```
2. Add the project ViewLib to your project
3. Update your `build.gradle` to include the library

```gradle
dependencies {
    api project(':ViewLib')
}
```

## Getting Started

### Initialize the Library

Before using RadaeePDF, initialize the library with your license key:

1. Modify the 'Global.java' file in the 'com.radaee.comm' package, to add your license key:
```java
public static String mKey = "[YOUR-LICENSE-KEY]";
```

2. Add the following code to your 'Application' class to initialize and activate the library:
```java
import com.radaee.pdf.Global;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize and activate RadaeePDF
        Global.Init(this);
    }
}
```

### Open and Display a PDF

#### Java

```java
import com.radaee.pdf.Document;
import com.radaee.reader.PDFEditLayoutView;
import com.radaee.view.IPDFLayoutView;
public class MainActivity extends AppCompatActivity implements IPDFLayoutView.PDFLayoutListener {
    private PDFLPDFEditLayoutViewayoutView pdfView;
    private Document doc;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        pdfView = findViewById(R.id.pdf_view);
        
        // Open PDF document
        doc = new Document();
        int ret = doc.Open("/sdcard/sample.pdf", null);

        switch(ret){
            case 0:
                // Display in PDFGLLayoutView
                pdfView.PDFOpen(doc, MainActivity.this);
                break;
            case -1:
                // Show dialog to input password
                break;
            case -2:
                // Unknown encryption error
                break;
            case -3:
                // Damaged or invalid format
                break;
            case -10:
                // Access denied or invalid file path
                break;
            default:
                // Unknown error
                break;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doc != null) {
            doc.Close();
        }
    }
}
```

#### Kotlin

```kotlin
import com.radaee.pdf.Document
import com.radaee.reader.PDFEditLayoutView
import com.radaee.view.ILayoutView.PDFLayoutListener

class MainActivity : AppCompatActivity(), PDFLayoutListener {
    private lateinit var pdfView: PDFEditLayoutView
    private var doc: Document? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        pdfView = findViewById(R.id.pdf_view)
        
        // Open PDF document
        doc = Document()
        val ret = doc?.Open("/sdcard/sample.pdf", null): Int

        when (ret) {
            0 -> {
                // Display in PDFGLLayoutView
                pdfView.PDFOpen(doc, this@MainActivity)
            }
            -1 -> {
                // Show dialog to input password
            }
            -2 -> {
                // Unknown encryption error
            }
            -3 -> {
                // Damaged or invalid format
            }
            -10 -> {
                // Access denied or invalid file path
            }
            else -> {
                // Unknown error
            }
        }
    }
    
    override fun onDestroy() {
        pdfView?.PDFClose()

        if (doc != null && doc.IsOpened()) {
            doc.Close()
        }
        super.onDestroy()
    }
```

## Common Operations

### Get Page Count

```java
int pageCount = doc.GetPageCount();
```

### Navigate to a Specific Page

```java
pdfView.PDFGotoPage(5); // Go to page 5
```

### Zoom Control

```java
// Zoom in
float currentZoom = 1.0f;
float newZoom = currentZoom * 1.2f;
pdfView.PDFSetScale(newZoom);

// Zoom out
newZoom = currentZoom / 1.2f;
pdfView.PDFSetScale(newZoom);
```

### Enable Night Mode

```java
import com.radaee.comm.Global;

Global.g_dark_mode = true;
pdfView.invalidate(); // Redraw the view
```

### Set View Mode

```java
// Vertical scroll mode
pdfView.PDFSetView(0);

// Horizontal scroll mode
pdfView.PDFSetView(1);

// Single page mode
pdfView.PDFSetView(3);

// Dual page in landscape mode
pdfView.PDFSetView(4);
```

### Text Search (Professional License)

```java
// Start search
boolean caseSensitive = false;
boolean wholeWord = false;
pdfView.PDFFindStart("search term", caseSensitive, wholeWord);

// Find forward/backward occurrence
int dir = 1; // 1 = forward, -1 = backward
pdfView.PDFFind(dir);

// End search and reset
pdfView.PDFFindEnd();
```

### Text Highlighting (Professional License)

```java
// Highlight selected text
pdfView.PDFSetSelMarkup(0); // 0 = highlight

// Underline selected text
pdfView.PDFSetSelMarkup(1); // 1 = underline

// Strikeout selected text
pdfView.PDFSetSelMarkup(2); // 2 = strikeout

// Squiggly underline selected text
pdfView.PDFSetSelMarkup(4); // 4 = squiggly
```

### Add Annotations (Professional License)

```java
// Add a note annotation at point (0, 0)
Page page = doc.GetPage(0); // Get first page
if (page != null) {
    page.ObjsStart();
    float[] pt = new float[2];
    pt[0] = 0;
    pt[1] = 0;
    page.AddAnnotText(pt);

    //Close page and release holding resource
    page.Close();
}

// Remove annotation
Page page = doc.GetPage(0); // Get first page
if (page != null) {
    page.ObjsStart();
    Annotation annot = page.GetAnnot(0); // Get first annotation
    if (annot != null) {
        annot.RemoveFromPage();
    }
    page.Close();
}
```

### Save Document

```java
// Save changes to the same file
doc.Save();

// Save to a new file
boolean rem_sec = false; // Remove security information
doc.SaveAs("/sdcard/newfile.pdf", rem_sec);
```

## License Levels

RadaeePDF offers different license levels with varying features:

Visit [https://www.radaeepdf.com/](https://www.radaeepdf.com/) for detailed licensing information.

## Documentation

For complete API documentation and advanced features, visit:
- [RadaeePDF Support Portal](https://support.radaeepdf.com/)
- [Wiki](https://github.com/RadaeePDF-Jugaad/RadaeePDF-Master-Android/wiki)

## Support

For technical support and questions:
- Email: support@radaeepdf.com
- Website: [https://www.radaeepdf.com/](https://www.radaeepdf.com/)

## License

This SDK is commercial software. Please ensure you have a valid license before using it in production applications.

---

© 2026 RadaeePDF. All rights reserved.
