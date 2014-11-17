package com.mobandme.remotte.app.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by Txus Ballesteros on 29/10/14.
 */
public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    public DeviceAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, null);


        ((TextView)convertView.findViewById(android.R.id.text1)).setText(getItem(position).getName());
        ((TextView)convertView.findViewById(android.R.id.text2)).setText(getItem(position).getAddress());

        convertView.setTag(getItem(position));

        return convertView;
    }
}
