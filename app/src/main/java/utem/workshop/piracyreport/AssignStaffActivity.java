package utem.workshop.piracyreport;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.ResponseBody;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;
import utem.workshop.piracyreport.fragmentsAdapter.ImagePagerAdapter;
import utem.workshop.piracyreport.models.Report;
import utem.workshop.piracyreport.models.User;
import utem.workshop.piracyreport.utils.Constants;
import utem.workshop.piracyreport.utils.MailSender;

public class AssignStaffActivity extends AppCompatActivity {

    Report report;

    @BindView(R.id.image_pager)
    ViewPager image_pager;

    @BindView(R.id.txAddressLine)
    TextView txAddressLine;

    @BindView(R.id.txBrand)
    TextView txBrand;

    @BindView(R.id.txCategory)
    TextView txCategory;

    @BindView(R.id.txDescription)
    TextView txDescription;

    @BindView(R.id.txStaffName)
    TextView txStaffName;

    @BindView(R.id.txStatus)
    TextView txStatus;

    @BindView(R.id.btnAssign)
    Button btnAssign;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_staff);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null) {
            report = Parcels.unwrap(getIntent().getParcelableExtra(Constants.REPORT_OBJ));
        } else {
            finish();
        }

        if (!report.getAssigned().equals(Constants.NOT_ASSIGNED)) {
            btnAssign.setText("Re-assign staff");
        } else {
            btnAssign.setText("Assign Staff");
        }

        ArrayList<String> mURL = report.getImgURL();

        ImagePagerAdapter adapter = new ImagePagerAdapter(this, mURL);
        image_pager.setAdapter(adapter);

        txAddressLine.setText(report.getAddress());
        txBrand.setText(report.getBrand());
        txCategory.setText(report.getCategory());
        txDescription.setText(report.getDescription());
        txStaffName.setText(report.getAssigned());
        txStatus.setText(report.getStatus());

        final ArrayList<User> users = new ArrayList<>();
        final ArrayList<String> staffList = new ArrayList<>();

        final FirebaseDatabase mDataRef = FirebaseDatabase.getInstance();
        Query queryRef = mDataRef.getReference("users").orderByChild("isAdmin").equalTo(false);

        queryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                users.clear();
                staffList.clear();

                // Add Unassigned
                User unassignUser = new User(Constants.NOT_ASSIGNED);
                unassignUser.setStaffUID("0x00");
                users.add(unassignUser);
                staffList.add(Constants.NOT_ASSIGNED);

                for (DataSnapshot dbSnap : dataSnapshot.getChildren()) {
                    User user = dbSnap.getValue(User.class);
                    user.setStaffUID(dbSnap.getKey());
                    users.add(user);
                    staffList.add(user.getStaffName());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        final DatabaseReference mDBUpdate = mDataRef.getReference("reports")
                .child(report.getReportID());

        btnAssign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new MaterialDialog.Builder(AssignStaffActivity.this)
                        .title("Assign Report to Staff")
                        .items(staffList)
                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                /**
                                 * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
                                 * returning false here won't allow the newly selected radio button to actually be selected.
                                 **/

                                Map mergeUpdate = new HashMap();

                                if (which==0) {
                                    mergeUpdate.put("status", Constants.ACTION_QUEUE);
                                    mergeUpdate.put("assigned", Constants.NOT_ASSIGNED);
                                    mergeUpdate.put("assignedID", "0x00");
                                } else {
                                    mergeUpdate.put("status", Constants.ACTION_IN_PROGRESS);
                                    mergeUpdate.put("assigned", users.get(which).getStaffName());
                                    mergeUpdate.put("assignedID", users.get(which).getStaffUID());
                                }

                                mDBUpdate.updateChildren(mergeUpdate, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError == null) {
                                            Toast.makeText(AssignStaffActivity.this, "Report successfully assigned", Toast.LENGTH_LONG)
                                                    .show();
                                            finish();
                                        }
                                    }
                                });
                                return true;
                            }
                        })
                        .positiveText("Assign")
                        .show();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
