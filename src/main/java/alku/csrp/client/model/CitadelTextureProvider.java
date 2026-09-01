package alku.csrp.client.model;

import net.minecraft.resources.ResourceLocation;

/** Supplies the entity texture used by a Citadel-backed parasite model. */
public interface CitadelTextureProvider<T> {
    ResourceLocation texture(T entity);
}
