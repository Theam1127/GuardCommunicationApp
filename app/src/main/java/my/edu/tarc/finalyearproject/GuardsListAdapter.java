package my.edu.tarc.finalyearproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

public class GuardsListAdapter extends BaseAdapter implements ListAdapter {
    List<Guard> guardList;
    private Context context;

    public GuardsListAdapter(List<Guard> guardList, Context context) {
        this.guardList = guardList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return guardList.size();
    }

    @Override
    public Object getItem(int i) {
        return guardList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View v, ViewGroup viewGroup) {
        View view = v;
        if(view==null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.guard_list_view,null);
        }
        TextView guardName = (TextView)view.findViewById(R.id.tvGuardName);
        Button buttonCall = (Button)view.findViewById(R.id.buttonCallGuard);
        guardName.setText(guardList.get(i).getGuardName());
        buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"+guardList.get(i).getPhoneNumber()));
                context.startActivity(intent);
            }
        });
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String currentGuard = preferences.getString("guardID", "");
        if(guardList.get(i).getGuardID().equals(currentGuard)) {
            buttonCall.setBackgroundColor(Color.GRAY);
            buttonCall.setClickable(false);
        }
        return view;
    }
}
