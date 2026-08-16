package net.wti.game.api;

import xapi.model.api.Model;

/// Marks the schema role of durable game data represented by an XApi model.
///
/// The marker classifies a model interface, not the authority of each instance. A
/// server-owned keyed instance may be authoritative while another instance using the same
/// schema is a client replica.
public interface GameDataModel extends Model {
}
