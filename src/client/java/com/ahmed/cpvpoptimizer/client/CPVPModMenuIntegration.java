package com.ahmed.cpvpoptimizer.client;

import com.ahmed.cpvpoptimizer.client.ui.OptimizerScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class CPVPModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OptimizerScreen::new;
    }
}
