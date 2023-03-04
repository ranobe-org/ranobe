package org.ranobe.ranobe.ui.main;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.navigation.NavigationBarView;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(Ranobe.getThemeMode(getApplicationContext()));
        setContentView(binding.getRoot());

        binding.navbar.setOnItemSelectedListener(this);
        binding.navbar.setSelectedItemId(R.id.browse);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        int id = item.getItemId();

        if (id == R.id.browse) {
            navController.navigate(R.id.browse_fragment);
        } else if (id == R.id.library) {
            navController.navigate(R.id.library_fragment);
        } else if (id == R.id.search) {
            navController.navigate(R.id.search_fragment);
        } else if (id == R.id.explore) {
            navController.navigate(R.id.explore_fragment);
        } else if (id == R.id.settings) {
            navController.navigate(R.id.settings_fragment);
        }

        return true;
    }
}