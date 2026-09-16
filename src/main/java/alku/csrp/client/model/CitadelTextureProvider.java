package alku.csrp.client.model;

import net.minecraft.resources.Identifier;

/** Supplies the entity texture used by a Citadel-backed parasite model. */
public interface CitadelTextureProvider<T> {
    Identifier texture(T entity);
}
