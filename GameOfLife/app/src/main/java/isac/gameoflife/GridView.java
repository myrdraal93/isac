package isac.gameoflife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Francesco on 07/03/2017.
 */

public class GridView extends View {

    private final static int TIME_DOUBLE_TAP=180;
    private final static int DESIRED_DP_VALUE=50;
    private float SIZE;
    private final float SIZE_INCHES = 0.520833333f;
    private Handler handler;
    private int width;
    private int height;
    private int row;
    private int column;
    private int startX;
    private int startY;
    private int numberOfTaps ;

    private Paint whitePaint = new Paint();
    private boolean[][] cellChecked;
    private boolean onTable;
    private String ipAddress;
    private MainActivity activity;
    private float xDots,yDots;
    //private PinchInfo.Direction direction;
    //se uso i lock, si blocca il thread UI, meglio utilizzare AtomicBoolean che permette
    //di effettuare operazioni thread-safe sui booleani
    private AtomicBoolean started=new AtomicBoolean(false),clear=new AtomicBoolean(false);
    private Long lastTapTimeMs,touchDownMs ;
    //il primo pair sono il timestamp e la direzione, il secondo le coordinate x e y
    private Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> infoSwipe;
    private ReentrantLock lockInfoSwipe,lockAction;

    public GridView(final Context context) {
        super(context);

        activity=(MainActivity)context;
        //imposto il colore delle celle
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
        ipAddress=Utils.getIpAddress();
        numberOfTaps=0;
        lastTapTimeMs=0L;
        touchDownMs=0L;
        onTable=false;
        //float scale = getResources().getDisplayMetrics().density;
        SIZE =  /*DESIRED_DP_VALUE * scale + 0.5f*/ DESIRED_DP_VALUE*(getResources().getDisplayMetrics().densityDpi/160.0f);
       /* switch(getResources().getDisplayMetrics().densityDpi){
            case 120: SIZE = 120* 50 + 0.5f; break;
            case 160: SIZE = 160*50+ 0.5f; break;
            case 213: SIZE = 213*50+ 0.5f; break;
            case 240: SIZE = 240*50+ 0.5f; break;
            case 320: SIZE = 320*50+ 0.5f; break;
            case 480: SIZE = 480*50+ 0.5f; break;
            case 640: SIZE = 640*50+ 0.5f; break;
        }*/


        //c'è modo di usarlo?
        float inches = 30/25.4f;
        //SIZE = inches;
        float xdpi = getResources().getDisplayMetrics().xdpi;
        xDots = inches * xdpi;
        float ydpi = getResources().getDisplayMetrics().ydpi;
        yDots = inches * ydpi;

        lockInfoSwipe=new ReentrantLock();
        lockAction=new ReentrantLock();
        handler=new Handler(this,activity,getWidth(),getHeight());

        Toast.makeText(context, Utils.getAddress(), Toast.LENGTH_SHORT).show();

        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... params) {

                if(handler.connectToServer()) {
                    System.out.println("Connessione riuscita");
                    handler.bindToBroadcastQueue();
                }else{
                    System.out.println("Connessione non riuscita");
                }

                return null;
            }

        }.execute();

        SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(new SensorEventListener() {

            private float oldX=Float.MAX_VALUE-0.5f,oldY=Float.MAX_VALUE-0.5f,oldZ=Float.MAX_VALUE-0.5f;

            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double g=Math.sqrt(x * x + y * y + z * z);

                x/=g;
                y/=g;
                z/=g;

                int inclination = (int) Math.round(Math.toDegrees(Math.acos(z)));

                if((inclination<=15)&&x>=(oldX-0.5)&&x<=(oldX+0.5)
                        &&y>=(oldY-0.5)&&y<=(oldY+0.5)){
                    if(!onTable) {
                        onTable = true;
                        System.out.println("sono sul tavolo Z: " + z + " X: " + x + " Y: " + y+" Inclination: "+inclination);
                    }
                }else{
                    if(onTable) {
                        onTable = false;
                        System.out.println("non sono sul tavolo Z: " + z + " X: " + x + " Y: " + y+" Inclination: "+inclination);

                        if(handler.isConnected()){
                            Toast.makeText(context, "Schermo scollegato", Toast.LENGTH_SHORT).show();
                            handler.closeDeviceCommunication();
                        }
                    }
                }


                oldX=x;
                oldY=y;
                oldZ=z;


            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

        }, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public float getCellSize(){
       return this.SIZE;
    }

    public /*Pair<Long,PinchInfo.Direction>*/Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> getInfoSwipe(){
        lockInfoSwipe.lock();

        /*Pair<Long,PinchInfo.Direction>*/Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> tmp;

        if(infoSwipe!=null) {
            tmp=new Pair<>(new Pair<>(infoSwipe.first.first,infoSwipe.first.second), new Pair<>(infoSwipe.second.first,infoSwipe.second.second));
        }else{
            tmp=null;
        }


        lockInfoSwipe.unlock();

        return tmp;
    }

    public boolean isStarted(){
        return started.get();
    }

    public void start(){
        //se il gioco non è stato già avviato, lo avvio eseguendo
        //un async task (necessario in quanto il thread UI non si deve bloccare)
        //se l'espressione booleana è false (primo parametro), imposto a true la variabile e proseguo
        lockAction.lock();

        if(started.compareAndSet(false,true)){
            new CalculateGeneration().start();
        }

        lockAction.unlock();
    }

    //metto in pausa il gioco
    public void pause(){
        //se il gioco è in esecuzione, setto a false la variabile
        lockAction.lock();

        started.compareAndSet(true,false);

        lockAction.unlock();
    }

    public void clear(){
        lockAction.lock();

        if(!isStarted()){
            cellChecked=new boolean[row+2][column+2];
            postInvalidate();
        }else {
            clear.set(true);
            pause();
        }

        lockAction.unlock();
    }

    //disegno la griglia e la popolo
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int count=0;

        //disegno delle righe per formare la griglia
        while(count<=row){
            float coordinate=count*SIZE;
            canvas.drawLine(coordinate,0,coordinate,column*SIZE,whitePaint);
           // canvas.drawRect(coordinate,0,xDots,yDots,whitePaint);
            count++;
        }


        count=0;

        while(count<=column){
            float coordinate=count*SIZE;
            canvas.drawLine(0,coordinate,row*SIZE,coordinate,whitePaint);
            //canvas.drawRect(0,coordinate,xDots,yDots,whitePaint);
            count++;
        }



        //setto le cellule vive
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                if (cellChecked[i+1][j+1]) {
                    //disegno un rettangolo in corrispondenza della cellula viva
                    canvas.drawRect(i * SIZE, j * SIZE,(i + 1) * SIZE, (j + 1) * SIZE,whitePaint);
                }
            }
        }
    }

    //metodo che rileva i tocchi sullo schermo
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            //l'utente ha il dito appoggiato sullo schermo
            case (MotionEvent.ACTION_DOWN) :
                touchDownMs = System.currentTimeMillis();
                startX=(int)event.getX();
                startY=(int)event.getY();
                return true;
            //l'utente sta muovendo il dito
            case (MotionEvent.ACTION_MOVE) :
                return true;
            //l'utente ha rilasciato il dito
            case (MotionEvent.ACTION_UP) :
                int stopX=(int)event.getX();
                int stopY=(int)event.getY();


                if ((System.currentTimeMillis() - touchDownMs) > TIME_DOUBLE_TAP) {
                    numberOfTaps = 0;
                    lastTapTimeMs = 0L;

                    //se la differenza delle coordinate di inizio e fine del movimento è minore di 3, allora
                    //l'utente vuole "attivare" una cella della griglia. altrimenti, potrebbe essere uno swipe
                    //per lo swipe controllare che il movimento sia lungo solo uno dei due assi e non entrambi
                    // (altrimenti mi sto muovendo in diagonale)
                    if (Math.abs(startX - stopX) < 10 && Math.abs(startY - stopY) < 10 && !started.get()) {
                        int column = (int) (event.getX() / SIZE);
                        int row = (int) (event.getY() / SIZE);

                        if(column<this.row && row<this.column) {
                            column+=1;
                            row+=1;

                            cellChecked[column][row] = !cellChecked[column][row];
                            //chiamo il metodo invalidate così forzo la chiamata del metodo onDraw
                            invalidate();
                        }

                    } else { //valuto lo swipe
                        long timeStamp = System.currentTimeMillis();
                        PinchInfo.Direction direction=null;

                        if (Math.abs(startX - stopX) >=10 && Math.abs(startY - stopY) <= 50){//se mi sono mosso sulle X
                            if((stopX - startX) > 0){
                                direction=PinchInfo.Direction.RIGHT;
                                Toast.makeText(getContext(), "Asse X destra", Toast.LENGTH_SHORT).show();
                                System.out.println("Destra su X");
                            } else if ((stopX - startX)<0){
                                direction=PinchInfo.Direction.LEFT;
                                Toast.makeText(getContext(), "Asse X sinistra", Toast.LENGTH_SHORT).show();
                            }

                            sendBroadcastMessage(timeStamp,direction,stopX,stopY);

                        } else if (Math.abs(startX - stopX) <=50 && Math.abs(startY - stopY) >= 10){//mi sono mosso sulle Y
                            if((stopY - startY) > 0){
                                direction=PinchInfo.Direction.DOWN;
                                Toast.makeText(getContext(), "Asse Y basso", Toast.LENGTH_SHORT).show();
                            } else if ((stopY - startY)<0){
                                direction=PinchInfo.Direction.UP;
                                Toast.makeText(getContext(), "Asse Y alto", Toast.LENGTH_SHORT).show();
                            }

                            sendBroadcastMessage(timeStamp,direction,stopX,stopY);
                        } else {
                            System.out.println("Mossa in diagonale");
                        }

                    }

                }

                if (numberOfTaps > 0
                        && (System.currentTimeMillis() - lastTapTimeMs) < TIME_DOUBLE_TAP) {
                    numberOfTaps += 1;
                } else {
                    numberOfTaps = 1;
                }

                lastTapTimeMs = System.currentTimeMillis();

                if (numberOfTaps == 3) {
                    System.out.println("Triplo tap");
                    Toast.makeText(getContext(), "Reset", Toast.LENGTH_SHORT).show();

                    clear();

                    if(handler.isConnected()) {

                        JSONObject message = new JSONObject();
                        try {
                            message.put("type", "reset");
                            message.put(PinchInfo.ADDRESS,ipAddress);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        handler.sendBroadcastMessage(message);
                    }

                } else if (numberOfTaps == 2) {
                    System.out.println("Doppio tap");



                    if(isStarted()){
                        Toast.makeText(getContext(), "Pause", Toast.LENGTH_SHORT).show();
                        System.out.println("Pausa");

                        pause();

                        if(handler.isConnected()){

                            JSONObject message=new JSONObject();
                            try {
                                message.put("type","pause");
                                message.put(PinchInfo.ADDRESS,ipAddress);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            handler.sendBroadcastMessage(message);
                        }
                    }else{
                        Toast.makeText(getContext(), "Start", Toast.LENGTH_SHORT).show();
                        System.out.println("Inizio");

                        start();

                        if(handler.isConnected()){

                            JSONObject message=new JSONObject();
                            try {
                                message.put("type","start");
                                message.put(PinchInfo.ADDRESS,ipAddress);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            handler.sendBroadcastMessage(message);
                        }
                    }

                }

                return true;
            case (MotionEvent.ACTION_CANCEL) :
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(changed) {
            width = getWidth();
            height = getHeight();
            row = /*width % SIZE == 0 ?*/(int) (width /SIZE) ;//: (width / SIZE) + 1;
            column = /*height % SIZE == 0 ?*/ (int)(height /SIZE);// : (height / SIZE) + 1;
            cellChecked = new boolean[row+2][column+2];
        }

    }


    public boolean[][] getCellMatrix(){
        return this.cellChecked;
    }


    /**
     * The final task of our project. Sets the outer border cells, where 2 devices are in contact.
     * The direction of the swipe is essential: you need to recognise what portion of screen
     * corresponds to one specific neighbour.
     * @param firstIndex
     * @param lastIndex
     * @param cells
     * @param direction the direction of the CURRENT device swipe
     */
    public void setPairedCells(int firstIndex, int lastIndex, List<Boolean> cells, PinchInfo.Direction direction){
        switch(direction){
            case RIGHT:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    cellChecked[column][i] = cells.get(j);
                };
                break;
            case LEFT:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    cellChecked[0][i] = cells.get(j);
                };
                break;
            case UP:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    cellChecked[i][0] = cells.get(j); //TODO: VERIFY- la riga 0 è in cima o in fondo?
                };
                break;
            case DOWN:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    cellChecked[i][row] = cells.get(j);//TODO: VERIFY- la riga 0 è in cima o in fondo?
                };
                break;
        }

    }

    private void sendBroadcastMessage(Long timeStamp,PinchInfo.Direction direction,int x,int y){
        lockInfoSwipe.lock();

        infoSwipe=new Pair<>(new Pair<>(timeStamp,direction),new Pair<>(x,y));

        lockInfoSwipe.unlock();

        handler.sendBroadcastMessage(new PinchInfo(ipAddress, direction,x,y,timeStamp, width, height).toJSON());
    }

    //async task che si occupa del calcolo delle generazioni di cellule
    private class CalculateGeneration extends Thread {

        private int neighboursAlive(int i,int j){
            //calcolo il numero di vicini vivi
            int neighbours=0;
            if((i-1)>=0){
                if((j-1)>=0){
                    if(cellChecked[i-1][j-1]){
                        neighbours++;
                    }
                }

                if(cellChecked[i-1][j]){
                    neighbours++;
                }

                if((j+1)<column+2){
                    if(cellChecked[i-1][j+1]){
                        neighbours++;
                    }
                }
            }


            if((j-1)>=0){
                if(cellChecked[i][j-1]){
                    neighbours++;
                }
            }

            if((j+1)<column+2){
                if(cellChecked[i][j+1]){
                    neighbours++;
                }
            }

            if((i+1)<row+2){
                if((j-1)>=0){
                    if(cellChecked[i+1][j-1]){
                        neighbours++;
                    }
                }

                if(cellChecked[i+1][j]){
                    neighbours++;
                }

                if((j+1)<column+2){
                    if(cellChecked[i+1][j+1]){
                        neighbours++;
                    }
                }
            }

            return neighbours;
        }


        private void calculateNextGen(){
            boolean [][] tmp=new boolean[row+2][column+2];

            for(int i=1;i<row+1;i++){
                for(int j=1;j<column+1;j++){
                    int neighbours=neighboursAlive(i,j);

                    //se attualmente la cellula è viva
                    if(cellChecked[i][j]) {
                        //e ha 2 o 3 vicini, continua a vivere
                        if (neighbours==2 || neighbours==3) {
                            tmp[i][j] = true;
                        }
                    }else{
                        //se la cellula è morta e ha esattamente 3 vicini
                        //nella generazione successiva prende vita
                        if(neighbours==3){
                            tmp[i][j]=true;
                        }
                    }
                }
            }

            cellChecked=tmp;
            //forzo la chiamata del metodo onDraw
            postInvalidate();

        }

        @Override
        public void run() {
            boolean goOn=true;

            while(goOn){
                //boolean [][] tmp=new boolean[row+2][column+2];

                if(handler.isConnected()){

                    //invio ai miei vicini le celle
                    handler.sendCellsToOthers();

                    //controllo se posso proseguire (ovvero ho ricevuto le celle da tutti i vicini)
                    while(!handler.goOn()){
                        // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    handler.resetReceived(); //resetto il contatore dei device che mi hanno inviato le celle
                    calculateNextGen(); //calcolo la generazione
                    handler.readyToContinue(); //invio un messaggio ai miei vicini con lo scopo di avvisarli che sono pronto a inviare le mie celle

                    // faccio il while fino a quando tutti i miei vicini
                    // non hanno terminato di calcolare la propria generazione
                    while(!handler.readyToSendCells()){
                        // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    handler.resetReceivedReady(); //resetto il contatore
                } else {
                    calculateNextGen();
                }


                //se l'utente non ha messo in pausa il gioco
                if(started.get()){
                    try {
                        Thread.sleep(handler.isConnected()?100:500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    //altrimenti fermo il calcolo delle generazione successiva
                    goOn=false;
                }
            }

            //una volta terminato il task, controllo
            //se l'utente ha richiesto un reset della griglia
            if(clear.compareAndSet(true,false)){
                clear();
            }
        }
    }
}
