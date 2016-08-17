package sampleapp.loop.ms.locations;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ms.loop.loopsdk.core.LoopSDK;
import ms.loop.loopsdk.profile.Drive;
import ms.loop.loopsdk.profile.Drives;
import ms.loop.loopsdk.profile.GeospatialPoint;
import ms.loop.loopsdk.profile.KnownLocation;
import ms.loop.loopsdk.profile.Locations;
import ms.loop.loopsdk.profile.Path;
import ms.loop.loopsdk.profile.Trip;
import ms.loop.loopsdk.profile.Trips;
import ms.loop.loopsdk.profile.Visit;
import sampleapp.loop.ms.locations.utils.LocationView;
import sampleapp.loop.ms.locations.utils.VisitView;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String entityId;
    private Locations locations;

    KnownLocation knownLocation;
    LocationView locationView;
    private ImageView backAction;
    //private ImageView deleteDriveAction;

    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    final SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private VisitsViewAdapter adapter;
    private ListView visitListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        entityId = this.getIntent().getExtras().getString("locationid");
        locations = Locations.createAndLoad(Locations.class, KnownLocation.class);

        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        locationView = new LocationView(viewGroup);

        backAction = (ImageView)findViewById(R.id.action_back_ic);
       // deleteDriveAction = (ImageView)findViewById(R.id.action_delete_drive_ic);

        backAction.setClickable(true);

        backAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        knownLocation = locations.byEntityId(entityId);
        if (knownLocation == null) return;

        adapter = new VisitsViewAdapter(this,
                R.layout.visitview, knownLocation.visits.getVisits());

        visitListView = (ListView) findViewById(R.id.visitlist);
        visitListView.setAdapter(adapter);

       /* deleteDriveAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (knownLocation != null){
                knownLocation.delete();
                finish();
            }
            }
        });*/

        viewGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

               /* // Adjust start time by 30 minutes
                Calendar startTime = Calendar.getInstance();
                startTime.setTime(trip.startedAt);
                startTime.add(Calendar.MINUTE, -30);

                Calendar endTime = Calendar.getInstance();
                endTime.setTime(trip.endedAt);
                endTime.add(Calendar.MINUTE, 30);

                String startedAtDate = dateFormat.format(startTime.getTime());
                String startedAtHour = hourFormat.format(startTime.getTime());
                String endedAtDate = dateFormat.format(endTime.getTime());
                String endedAtHour = hourFormat.format(endTime.getTime());
                String queryDate = LoopSDK.userId + " AND location AND createdAt:[\"" + startedAtDate + "T" + startedAtHour + "-07:00\" TO \"" + endedAtDate + "T" + endedAtHour + "-07:00\"]";
                clipboard.setText(queryDate);
                Toast.makeText(MapsActivity.this, "Elastic search query copied", Toast.LENGTH_SHORT).show();*/
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        knownLocation = locations.byEntityId(entityId);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        drawPath();
    }

    public void drawPath() {

        locationView.update(this, knownLocation);

        GeospatialPoint firstPoint = new GeospatialPoint(knownLocation.getLocation());

        PolylineOptions options = new PolylineOptions()
                .add(new LatLng(firstPoint.latDegrees,firstPoint.longDegrees))
                .width(10)
                .color(Color.BLUE)
                .geodesic(true).clickable(true);

        MarkerOptions startMarker = new MarkerOptions();
        startMarker.position(new LatLng(firstPoint.latDegrees,firstPoint.longDegrees)).title(knownLocation.labels.getLabels().get(0).name);
        startMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.location));

        mMap.addMarker(startMarker);
        LatLng latLng = new LatLng(firstPoint.latDegrees, firstPoint.longDegrees);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        for (KnownLocation location: locations.sortedByScore()) {
            if (location.entityId.equals(knownLocation.entityId)) continue;
            latLng = new LatLng(location.latDegrees,location.longDegrees);
            MarkerOptions endMarker = new MarkerOptions();
            endMarker.position(latLng).title(knownLocation.labels.getLabels().get(0).name);
            endMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.location));
            mMap.addMarker(endMarker);
        }
    }

    class VisitsViewAdapter extends ArrayAdapter<Visit> {

        Context context;
        int layoutResourceId;
        List<Visit> visits = new ArrayList<>();

        public VisitsViewAdapter(Context context, int layoutResourceId, List<Visit> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            update(data);
        }

        public void update(List<Visit> data) {

            visits = data;
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return visits.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            VisitView holder = null;

            if(row == null) {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new VisitView(row);
                row.setTag(holder);
                row.setClickable(true);
            }
            else {
                holder = (VisitView) row.getTag();
            }

            if (visits.isEmpty()) return row;
            final Visit visit = visits.get(position);
            if (visit == null ) return row;
            holder.update(context, visit);
            return row;
        }
    }
}
