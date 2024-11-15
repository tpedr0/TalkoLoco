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

        // Initialize dropdown options
        dropDownOptions = new ArrayList<>();
        dropDownOptions.add("WhatsApp is buttcheeks compared to this!");
        dropDownOptions.add("Feeling happy :D");
        dropDownOptions.add("Busy.");
        dropDownOptions.add("Away.");
        dropDownOptions.add("Do not Disturb");

        // Initialize the adapter with the context, layout, and options
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dropDownOptions);


        // Reference the AutoCompleteTextView and set the adapter
        AutoCompleteTextView dropdown = findViewById(R.id.dropdown);
        dropdown = findViewById(R.id.dropdown);
        dropdown.setText("WhatsApp is buttcheeks compared to this!");
        dropdown.setAdapter(adapter);
    }

    // Method to add a new item to the dropdown list
    private void addItemToDropdown(String item) {
        if (!dropDownOptions.contains(item)) { // Avoid duplicates
            dropDownOptions.add(item);         // Add to ArrayList
            adapter.notifyDataSetChanged();    // Notify adapter of data change
        }
    }
}
