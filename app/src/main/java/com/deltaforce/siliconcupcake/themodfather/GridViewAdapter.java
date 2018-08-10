package com.deltaforce.siliconcupcake.themodfather;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class GridViewAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> characters;
    private ArrayList<Integer> selections;

    public GridViewAdapter(Context context, ArrayList<String> characters) {
        this.context = context;
        this.characters = characters;
        selections = new ArrayList<>();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        convertView = inflater.inflate(R.layout.grid_item, null);
        if (selections.contains(position)) {
            convertView.setBackground(context.getResources().getDrawable(R.drawable.card_selected, null));
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selections.contains(position)) {
                    view.setBackground(context.getResources().getDrawable(R.drawable.card_normal, null));
                    selections.remove(Integer.valueOf(position));
                } else {
                    view.setBackground(context.getResources().getDrawable(R.drawable.card_selected, null));
                    selections.add(position);
                }
            }
        });

        TextView content = convertView.findViewById(R.id.character_type);
        content.setText(characters.get(position));

        return convertView;
    }

    @Override
    public int getCount() {
        return characters.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public ArrayList<Integer> getSelections() {
        return selections;
    }

}
