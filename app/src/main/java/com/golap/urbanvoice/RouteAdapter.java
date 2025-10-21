package com.golap.urbanvoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<Route> routeList;
    private final OnRouteClickListener listener;

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
    }

    public RouteAdapter(List<Route> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.listener = listener;
    }

    public void filterList(List<Route> filteredList) {
        routeList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);

        holder.typeNumberTextView.setText(route.getDisplayText());
        holder.descriptionTextView.setText(route.getDescription());

        if (route.getType().equals("Тролейбус")) {
            holder.iconImageView.setImageResource(R.drawable.trolleybus_icon);
        } else if (route.getType().equals("Автобус")) {
            holder.iconImageView.setImageResource(R.drawable.bus_icon);
        } else if (route.getType().equals("Трамвай")) {
            holder.iconImageView.setImageResource(R.drawable.tram_icon);
        }

        holder.itemView.setOnClickListener(v -> listener.onRouteClick(route));
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconImageView;
        final TextView typeNumberTextView;
        final TextView descriptionTextView;

        RouteViewHolder(View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.route_icon);
            typeNumberTextView = itemView.findViewById(R.id.route_type_number);
            descriptionTextView = itemView.findViewById(R.id.route_description);
        }
    }
}
