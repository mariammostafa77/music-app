package com.example.musicapp;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder>{
    private Context mContext;
    private ArrayList<MusicFiles>mFiles;

    MusicAdapter(Context mContext,ArrayList<MusicFiles>mFiles){
        this.mFiles=mFiles;
        this.mContext=mContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(R.layout.music_items,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.file_name.setText(mFiles.get(position).getTitle());
        byte[] image=getAlbumArt(mFiles.get(position).getPath());
        if(image!=null){
            Glide.with(mContext).asBitmap()
                    .load(image).
                    into(holder.album_art);
        }
        else{
            Glide.with(mContext)
                    .load(R.drawable.music)
                    .into(holder.album_art);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(mContext,PlayerActivity.class);
                intent.putExtra("position",position);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView file_name;
        ImageView album_art;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name=itemView.findViewById(R.id.music_file_name);
            album_art=itemView.findViewById(R.id.music_img);
        }
    }
    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            if (Build.VERSION.SDK_INT >= 14)
                retriever.setDataSource(uri, new HashMap<String, String>());
            else
                retriever.setDataSource(uri);
        }catch (Exception e){

        }
        //MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        //retriever.setDataSource(uri);
        byte[] art=retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }
}
