package com.exeai.assistant;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private LinearLayout chat;
    private EditText input;
    private ScrollView scroll;
    private DB db;
    private TextToSpeech tts;
    private static final int SPEECH=41, IMAGE=42;
    private final int RED=Color.rgb(255,45,55);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(9,9,9));
        getWindow().setNavigationBarColor(Color.rgb(9,9,9));
        db=new DB(this);
        tts=new TextToSpeech(this, status -> { if(status==TextToSpeech.SUCCESS) tts.setLanguage(new Locale("tr","TR")); });
        buildUI();
        addMessage("EXE AI 3.0 hazır. Çevrimdışı bilgi, öğrenme hafızası, sesli giriş, konuşma ve OCR araçları kullanılabilir.", false);
    }

    private void buildUI() {
        LinearLayout base=new LinearLayout(this); base.setOrientation(1); base.setBackgroundColor(Color.rgb(10,10,10));
        TextView title=new TextView(this); title.setText("EXE AI 3.0"); title.setTextColor(RED); title.setTextSize(26); title.setTypeface(null,1); title.setPadding(18,18,12,4);
        TextView sub=new TextView(this); sub.setText("PERSONAL ASSISTANT  •  OFFLINE MEMORY"); sub.setTextColor(0xffaaaaaa); sub.setTextSize(10); sub.setPadding(20,0,12,12);
        base.addView(title); base.addView(sub);
        scroll=new ScrollView(this); chat=new LinearLayout(this); chat.setOrientation(1); chat.setPadding(12,10,12,10); scroll.addView(chat);
        base.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout tools=new LinearLayout(this); tools.setPadding(8,4,8,4);
        addTool(tools,"🎙",v->listen()); addTool(tools,"📷 OCR",v->pickImage()); addTool(tools,"▶ YouTube",v->launchApp("youtube"));
        addTool(tools,"⚙ Ayarlar",v->startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));
        base.addView(tools);
        LinearLayout bottom=new LinearLayout(this); bottom.setPadding(8,8,8,12);
        input=new EditText(this); input.setSingleLine(false); input.setHint("EXE AI'ye yaz..."); input.setTextColor(Color.WHITE); input.setHintTextColor(0xff888888); input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RED)); bottom.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        Button send=new Button(this); send.setText("GÖNDER"); send.setTextColor(Color.WHITE); send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffb00020)); send.setOnClickListener(v->send()); bottom.addView(send);
        base.addView(bottom); setContentView(base);
    }

    private void addTool(LinearLayout row,String label, android.view.View.OnClickListener click) {
        Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff242424)); b.setOnClickListener(click);
        row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void addMessage(String text, boolean user) {
        TextView t=new TextView(this); t.setText((user?"SEN  ›  ":"EXE  ›  ")+text); t.setTextColor(user?0xffffb5b5:Color.WHITE); t.setTextSize(15); t.setPadding(14,12,14,12);
        t.setBackgroundColor(user?0xff351316:0xff202020);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,5,0,5); chat.addView(t,lp);
        scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
    }
    private void send() {
        String q=input.getText().toString().trim(); if(q.isEmpty()) return;
        input.setText(""); addMessage(q,true); String a=answer(q); addMessage(a,false);
        if(tts!=null) tts.speak(a,TextToSpeech.QUEUE_FLUSH,null,"exe_reply");
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
    }
    private String answer(String q) {
        String s=q.toLowerCase(new Locale("tr","TR"));
        if(s.startsWith("öğren:") || s.startsWith("ogren:")) {
            String fact=q.substring(q.indexOf(':')+1).trim();
            if(fact.isEmpty()) return "Öğrenilecek bilgiyi iki nokta üst üste sonrasına yaz.";
            db.save(fact,"user"); return "Bilgiyi yerel hafızama kaydettim: "+fact;
        }
        if(s.contains("youtube aç") || s.equals("youtube")) { launchApp("youtube"); return "YouTube açma isteği gönderildi."; }
        if(s.contains("minecraft aç") || s.equals("minecraft")) { launchApp("minecraft"); return "Minecraft açma isteği gönderildi. Kurulu değilse açılmaz."; }
        if(s.contains("wifi") || s.contains("wi-fi")) { startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)); return "Wi‑Fi ayarları açıldı."; }
        if(s.contains("bluetooth")) { startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)); return "Bluetooth ayarları açıldı."; }
        if(s.contains("saat kaç")) return "Cihaz saati: "+new java.text.SimpleDateFormat("HH:mm",new Locale("tr","TR")).format(new Date());
        if(s.contains("merhaba") || s.contains("selam")) return "Merhaba! Ne yapmak istersin?";
        if(s.contains("yardım") || s.equals("help")) return "Komutlar: 'öğren: ...' ile hafızaya bilgi ekle; 'YouTube aç', 'Minecraft aç', 'Wi-Fi ayarları', 'Bluetooth ayarları'. Mikrofonla konuşabilir veya OCR ile görseldeki yazıyı okuyabilirsin.";
        String remembered=db.find(q);
        if(remembered!=null) return "Hafızamda bulduğum bilgi: "+remembered;
        String canned=db.knowledge(s);
        if(canned!=null) return canned;
        return "Bu konuda yerel bilgi tabanımda henüz yanıt yok. İnternet bağlantısı olan sürümlerde çevrimiçi araştırma modülü eklenebilir. Yeni bilgi öğretmek için 'öğren: bilgi' yaz.";
    }
    private void launchApp(String name) {
        String[] pkgs=name.equals("youtube")?new String[]{"com.google.android.youtube"}:new String[]{"com.mojang.minecraftpe","com.mojang.minecrafttrialpe"};
        for(String p:pkgs) { Intent i=getPackageManager().getLaunchIntentForPackage(p); if(i!=null){startActivity(i);return;} }
        Toast.makeText(this,"Uygulama bulunamadı veya kurulu değil.",Toast.LENGTH_SHORT).show();
    }
    private void listen() {
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR"); i.putExtra(RecognizerIntent.EXTRA_PROMPT,"EXE AI'ye konuş");
        try { startActivityForResult(i,SPEECH); } catch(Exception e) { addMessage("Bu cihazda konuşma tanıma servisi bulunamadı.",false); }
    }
    private void pickImage() {
        Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*"); startActivityForResult(i,IMAGE);
    }
    @Override protected void onActivityResult(int req,int res,Intent data) {
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK || data==null) return;
        if(req==SPEECH) {
            ArrayList<String> r=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(r!=null&&!r.isEmpty()){input.setText(r.get(0));send();}
        } else if(req==IMAGE) {
            try {
                InputImage image=InputImage.fromFilePath(this,data.getData());
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener(result->addMessage("Görselde okunan yazı:\n"+result.getText(),false))
                    .addOnFailureListener(e->addMessage("OCR başarısız: "+e.getMessage(),false));
            } catch(Exception e) { addMessage("Görsel açılamadı.",false); }
        }
    }
    @Override protected void onDestroy() { if(tts!=null){tts.stop();tts.shutdown();} super.onDestroy(); }

    static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"exe_ai_memory.db",null,1);}
        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE memory(id INTEGER PRIMARY KEY AUTOINCREMENT, fact TEXT, tag TEXT, created INTEGER)");
            d.execSQL("CREATE TABLE qa(question TEXT PRIMARY KEY, answer TEXT)");
            seed(d);
        }
        public void onUpgrade(SQLiteDatabase d,int o,int n){}
        void save(String fact,String tag){ ContentValues v=new ContentValues();v.put("fact",fact);v.put("tag",tag);v.put("created",System.currentTimeMillis());getWritableDatabase().insert("memory",null,v);}
        String find(String q) {
            Cursor c=getReadableDatabase().rawQuery("SELECT fact FROM memory ORDER BY id DESC",null);
            try { while(c.moveToNext()){String f=c.getString(0); if(q.length()>2 && f.toLowerCase(new Locale("tr","TR")).contains(q.toLowerCase(new Locale("tr","TR")))) return f;} } finally {c.close();}
            return null;
        }
        String knowledge(String q) {
            Cursor c=getReadableDatabase().rawQuery("SELECT answer FROM qa WHERE lower(question)=lower(?)",new String[]{q});
            try{return c.moveToFirst()?c.getString(0):null;}finally{c.close();}
        }
        void put(SQLiteDatabase d,String q,String a){ContentValues v=new ContentValues();v.put("question",q);v.put("answer",a);d.insertWithOnConflict("qa",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        void seed(SQLiteDatabase d) {
            put(d,"ram nedir","RAM, cihazın o anda çalışan uygulama ve verileri geçici olarak tuttuğu bellektir.");
            put(d,"cpu nedir","CPU, komutları işleyen ana işlemci birimidir.");
            put(d,"gpu nedir","GPU, grafik işlemlerini hızlandıran işlemci birimidir.");
            put(d,"html nedir","HTML, web sayfalarının yapısını tanımlamak için kullanılan işaretleme dilidir.");
            put(d,"java nedir","Java, Android uygulamaları da geliştirilebilen nesne yönelimli bir programlama dilidir.");
            put(d,"minecraft bedrock nedir","Minecraft Bedrock, Android, konsol ve Windows gibi farklı platformlarda bulunan Minecraft sürümüdür.");
            put(d,"redstone nedir","Redstone, Minecraft'ta devreler ve otomasyon mekanizmaları kurmak için kullanılan sistemdir.");
            put(d,"internet nedir","İnternet, dünya çapındaki bilgisayar ağlarını birbirine bağlayan küresel ağdır.");
            put(d,"yapay zeka nedir","Yapay zekâ, bilgisayarların öğrenme, çıkarım ve dil işleme gibi görevleri gerçekleştirmesini sağlayan yöntemler alanıdır.");
        }
    }
          }
          
