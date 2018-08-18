package com.deltaforce.siliconcupcake.themodfather;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;

public class GridViewAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> characters;
    private ArrayList<Endpoint> players;
    private ArrayList<Integer> selections;
    private boolean isPlayers;

    public GridViewAdapter(Context context, Object characters, boolean isPlayers) {
        this.context = context;
        selections = new ArrayList<>();
        this.isPlayers = isPlayers;
        if (isPlayers)
            this.players = (ArrayList<Endpoint>) characters;
        else
            this.characters = (ArrayList<String>) characters;

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
        if (isPlayers)
            content.setText(players.get(position).getName());
        else {
            content.setText(characters.get(position));
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final Dialog alertDialog = new Dialog(context);
                    alertDialog.setContentView(R.layout.dialog_alert);
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    alertDialog.findViewById(R.id.dialog_title).setVisibility(View.VISIBLE);
                    ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText(characters.get(position));
                    ((TextView) alertDialog.findViewById(R.id.dialog_text)).setText(MafiaUtils.CHARACTER_HINTS.get(position));
                    alertDialog.findViewById(R.id.dialog_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.show();
                    return true;
                }
            });
        }

        return convertView;
    }

    @Override
    public int getCount() {
        if (isPlayers)
            return players.size();
        else
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
