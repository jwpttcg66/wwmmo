package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.TabManager.TabInfo;
import au.com.codeka.warworlds.game.EmpireActivity;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

/**
 * The \c StarfieldActivity is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldActivity extends Activity {
    private Context mContext = this;
    private StarfieldSurfaceView mStarfield;
    private ListView mPlanetList;
    private PlanetListAdapter mPlanetListAdapter;
    private ListView mFleetList;
    private FleetListAdapter mFleetListAdapter;
    private Star mSelectedStar;

    private static final int SOLAR_SYSTEM_REQUEST = 1;
    private static final int EMPIRE_REQUEST = 2;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.starfield);

        mStarfield = (StarfieldSurfaceView) findViewById(R.id.starfield);
        final View selectionLoadingContainer = findViewById(R.id.loading_container);
        final View selectedStarContainer = findViewById(R.id.selected_star);
        final View selectedFleetContainer = findViewById(R.id.selected_fleet);
        final ImageView fleetIcon = (ImageView) findViewById(R.id.fleet_icon);
        final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        final TextView fleetDesign = (TextView) findViewById(R.id.fleet_design);
        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        final TextView fleetDetails = (TextView) findViewById(R.id.fleet_details);
        final TextView starName = (TextView) findViewById(R.id.star_name);
        final ImageView starIcon = (ImageView) findViewById(R.id.star_icon);

        mPlanetList = (ListView) findViewById(R.id.planet_list);
        mFleetList = (ListView) findViewById(R.id.fleet_list);

        selectedStarContainer.setVisibility(View.GONE);
        selectedFleetContainer.setVisibility(View.GONE);

        MyEmpire empire = EmpireManager.getInstance().getEmpire();
        TextView username = (TextView) findViewById(R.id.username);
        username.setText(empire.getDisplayName());
        final TextView cashTextView = (TextView) findViewById(R.id.cash);
        cashTextView.setText(Cash.format(empire.getCash()));

        EmpireManager.getInstance().addEmpireUpdatedListener(empire.getKey(), new EmpireManager.EmpireFetchedHandler() {
            @Override
            public void onEmpireFetched(Empire empire) {
                cashTextView.setText(Cash.format(empire.getCash()));
            }
        });

        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
            if (starKey != null) {
                StarManager.getInstance().requestStar(starKey, false,
                        new StarManager.StarFetchedHandler() {
                    @Override
                    public void onStarFetched(Star s) {
                        mStarfield.scrollTo(s.getSectorX(), s.getSectorY(),
                                            s.getOffsetX(), s.getOffsetY(),
                                            true);
                    }
                });
            }
        }

        mPlanetListAdapter = new PlanetListAdapter();
        mPlanetList.setAdapter(mPlanetListAdapter);

        mFleetListAdapter = new FleetListAdapter();
        mFleetList.setAdapter(mFleetListAdapter);

        mStarfield.addSelectionChangedListener(new StarfieldSurfaceView.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
                // load the rest of the star's details as well
                selectionLoadingContainer.setVisibility(View.VISIBLE);
                selectedStarContainer.setVisibility(View.GONE);
                selectedFleetContainer.setVisibility(View.GONE);

                StarManager.getInstance().requestStar(star.getKey(), true,
                                                      new StarManager.StarFetchedHandler() {
                    /**
                     * This is called on the main thread when the star is actually fetched.
                     */
                    @Override
                    public void onStarFetched(Star star) {
                        mSelectedStar = star;
                        selectionLoadingContainer.setVisibility(View.GONE);
                        selectedStarContainer.setVisibility(View.VISIBLE);

                        mPlanetListAdapter.setStar(star);
                        mFleetListAdapter.setStar(star);

                        starName.setText(star.getName());
                        Sprite starImage = StarImageManager.getInstance().getSprite(mContext, star, 80);
                        starIcon.setImageDrawable(new SpriteDrawable(starImage));
                    }
                });
            }

            @Override
            public void onFleetSelected(Fleet fleet) {
                empireName.setText("");
                empireIcon.setImageBitmap(null);

                ShipDesign design = ShipDesignManager.getInstance().getDesign(fleet.getDesignID());
                EmpireManager.getInstance().fetchEmpire(fleet.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        empireName.setText(empire.getDisplayName());
                        empireIcon.setImageBitmap(empire.getShield(mContext));
                    }
                });

                fleetDesign.setText(design.getDisplayName());
                fleetIcon.setImageDrawable(new SpriteDrawable(design.getSprite()));

                String eta = "???";
                Star srcStar = SectorManager.getInstance().findStar(fleet.getStarKey());
                Star destStar = SectorManager.getInstance().findStar(fleet.getDestinationStarKey());
                if (srcStar != null && destStar != null) {
                    float timeRemainingInHours = fleet.getTimeToDestination(srcStar, destStar);
                    eta = TimeInHours.format(timeRemainingInHours);
                }

                String details = String.format(
                    "<b>Ships:</b> %d<br />" +
                    "<b>Speed:</b> %.2f pc/hr<br />" +
                    "<b>Destination:</b> %s<br />" +
                    "<b>ETA:</b> %s",
                    fleet.getNumShips(), design.getSpeedInParsecPerHour(),
                    (destStar == null ? "???" : destStar.getName()),
                    eta);
                fleetDetails.setText(Html.fromHtml(details));

                selectionLoadingContainer.setVisibility(View.GONE);
                selectedStarContainer.setVisibility(View.GONE);
                selectedFleetContainer.setVisibility(View.VISIBLE);
            }
        });

        mPlanetList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedStar == null) {
                    return; //??
                }

                Planet planet = null;
                if (position >= 0 && position < mSelectedStar.getPlanets().length) {
                    planet = mSelectedStar.getPlanets()[position];
                }

                navigateToPlanet(mSelectedStar, planet, false);
            }
        });

        final Button empireBtn = (Button) findViewById(R.id.empire_btn);
        empireBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, EmpireActivity.class);
                StarfieldActivity.this.startActivityForResult(intent, EMPIRE_REQUEST);
            }
        });
    }

    /**
     * Navigates to the given planet in the given star. Starts the SolarSystemActivity.
     * 
     * @param star
     * @param planet
     * @param scrollView If \c true, we'll also scroll the current view so that given star
     *         is centered on the given star.
     */
    public void navigateToPlanet(Star star, Planet planet, boolean scrollView) {
        navigateToPlanet(star.getSectorX(), star.getSectorY(), star.getKey(),
                         star.getOffsetX(), star.getOffsetY(), planet.getIndex(),
                         scrollView);
    }

    private void navigateToPlanet(long sectorX, long sectorY, String starKey, int starOffsetX,
                                  int starOffsetY, int planetIndex, boolean scrollView) {
        if (scrollView) {
            int offsetX = starOffsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
            int offsetY = starOffsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());
            mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY);
        }

        Intent intent = new Intent(mContext, SolarSystemActivity.class);
        intent.putExtra("au.com.codeka.warworlds.SectorX", sectorX);
        intent.putExtra("au.com.codeka.warworlds.SectorY", sectorY);
        intent.putExtra("au.com.codeka.warworlds.StarKey", starKey);
        intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planetIndex);
        startActivityForResult(intent, SOLAR_SYSTEM_REQUEST);
    }

    public void navigateToFleet(final String starKey, final String fleetKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        if (star == null) {
            StarManager.getInstance().requestStar(starKey, false, new StarManager.StarFetchedHandler() {
                @Override
                public void onStarFetched(Star s) {
                    Fleet fleet = s.findFleet(fleetKey);
                    navigateToFleet(s, fleet);
                }
            });
        } else {
            navigateToFleet(star, star.findFleet(fleetKey));
        }
    }

    public void navigateToFleet(Star star, Fleet fleet) {
        int offsetX = star.getOffsetX() - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
        int offsetY = star.getOffsetY() - (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

        // todo: if the fleet is moving, scroll to it...

        mStarfield.scrollTo(star.getSectorX(), star.getSectorY(), offsetX, offsetY);

        if (fleet.getState() == Fleet.State.MOVING) {
            mStarfield.selectFleet(fleet);
        } else {
            mStarfield.selectStar(star.getKey());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SOLAR_SYSTEM_REQUEST && intent != null) {
            boolean wasSectorUpdated = intent.getBooleanExtra(
                    "au.com.codeka.warworlds.SectorUpdated", false);
            long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
            String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

            if (wasSectorUpdated) {
                SectorManager.getInstance().refreshSector(sectorX, sectorY);
            } else {
                // make sure we re-select the star you had selected before.
                mStarfield.selectStar(starKey);
            }
        } else if (requestCode == EMPIRE_REQUEST && intent != null) {
            EmpireActivity.EmpireActivityResult res = EmpireActivity.EmpireActivityResult.fromValue(
                    intent.getIntExtra("au.com.codeka.warworlds.Result", 0));

            long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
            int starOffsetX = intent.getIntExtra("au.com.codeka.warworlds.StarOffsetX", 0);
            int starOffsetY = intent.getIntExtra("au.com.codeka.warworlds.StarOffsetY", 0);
            String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

            if (res == EmpireActivity.EmpireActivityResult.NavigateToPlanet) {
                int planetIndex = intent.getIntExtra("au.com.codeka.warworlds.PlanetIndex", 0);

                navigateToPlanet(sectorX, sectorY, starKey, starOffsetX, starOffsetY,
                                 planetIndex, true);
            } else if (res == EmpireActivity.EmpireActivityResult.NavigateToFleet) {
                String fleetKey = intent.getStringExtra("au.com.codeka.warworlds.FleetKey");

                navigateToFleet(starKey, fleetKey);
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d = DialogManager.getInstance().onCreateDialog(this, id);
        if (d == null)
            d = super.onCreateDialog(id);
        return d;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d, Bundle args) {
        DialogManager.getInstance().onPrepareDialog(this, id, d, args);
        super.onPrepareDialog(id, d, args);
    }

    class PlanetListAdapter extends BaseAdapter {
        private Star mStar;

        public void setStar(Star star) {
            mStar = star;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mStar == null) {
                return 0;
            }

            return mStar.getNumPlanets();
        }

        @Override
        public Object getItem(int position) {
            return mStar.getPlanets()[position];
        }

        @Override
        public long getItemId(int position) {
            return position; // TODO??
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);
            }

            final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
            final Planet planet = mStar.getPlanets()[position];
            final PlanetImageManager pim = PlanetImageManager.getInstance();

            Sprite sprite= pim.getSprite(mContext, planet);
            icon.setImageDrawable(new SpriteDrawable(sprite));

            TextView planetTypeTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
            planetTypeTextView.setText(planet.getPlanetType().getDisplayName());

            Colony colony = null;
            for(Colony c : mStar.getColonies()) {
                if (c.getPlanetIndex() == planet.getIndex()) {
                    colony = c;
                    break;
                }
            }

            final TextView colonyTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
            if (colony != null) {
                colonyTextView.setText("Colonized");
                EmpireManager.getInstance().fetchEmpire(colony.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        colonyTextView.setText(empire.getDisplayName());
                    }
                });
            } else {
                colonyTextView.setText("");
            }

            return view;
        }
    }

    class FleetListAdapter extends BaseAdapter {
        private Star mStar;
        private List<Fleet> mFleets;

        public void setStar(Star star) {
            mStar = star;
            mFleets = star.getFleets();
            if (mFleets == null) {
                mFleets = new ArrayList<Fleet>();
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mStar == null) {
                return 0;
            }

            return mFleets.size();
        }

        @Override
        public Object getItem(int position) {
            return mFleets.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position; // TODO??
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);
            }

            final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
            final Fleet fleet = mFleets.get(position);
            ShipDesignManager designManager = ShipDesignManager.getInstance();
            ShipDesign design = designManager.getDesign(fleet.getDesignID());

            icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

            TextView shipKindTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
            shipKindTextView.setText(String.format("%s (× %d)",
                                                   design.getDisplayName(),
                                                   fleet.getNumShips()));

            final TextView shipCountTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
            shipCountTextView.setText(String.format("%s",
                    StringUtils.capitalize(fleet.getStance().toString().toLowerCase())));

            return view;
        }
    }

    public class SelectedStarTabInfo extends TabManager.TabInfo {
        public SelectedStarTabInfo(String title) {
            super(title);
        }

        @Override
        public View createTabContent(String tag) {
            if (tag.equals("Planets")) {
                return mPlanetList;
            } else {
                return mFleetList;
            }
        }

        @Override
        public void switchTo(TabInfo lastTab) {
            if (title.equals("Planets")) {
                mPlanetList.setVisibility(View.VISIBLE);
                mFleetList.setVisibility(View.GONE);
            } else {
                mFleetList.setVisibility(View.VISIBLE);
                mPlanetList.setVisibility(View.GONE);
            }
        }

    }
}