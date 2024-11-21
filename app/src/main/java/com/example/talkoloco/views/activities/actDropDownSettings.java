package com.example.talkoloco.views.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;

import java.util.ArrayList;

public class actDropDownSettings extends AppCompatActivity {
    private ArrayList<String> dropDownOptions;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // initialize dropdown menu
        dropDownOptions = new ArrayList<>();
        dropDownOptions.add("WhatsApp is inferior compared to this!");
        dropDownOptions.add("Feeling happy :D");
        dropDownOptions.add("Busy.");
        dropDownOptions.add("Away.");
        dropDownOptions.add("Do not Disturb");

        // initialize the adapter with the context, layout, and options
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dropDownOptions);


        // reference the AutoCompleteTextView and set the adapter
        AutoCompleteTextView dropdown = findViewById(R.id.dropdown);
        dropdown = findViewById(R.id.dropdown);
        dropdown.setText("WhatsApp is buttcheeks compared to this!");
        dropdown.setAdapter(adapter);
    }

    //  add a new item to the dropdown list
    private void addItemToDropdown(String item) {
        if (!dropDownOptions.contains(item)) { // avoid duplicates
            dropDownOptions.add(item);         // add to ArrayList
            adapter.notifyDataSetChanged();    // notify adapter of data change
        }
    }
}
